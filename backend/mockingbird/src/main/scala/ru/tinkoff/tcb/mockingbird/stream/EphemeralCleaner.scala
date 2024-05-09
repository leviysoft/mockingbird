package ru.tinkoff.tcb.mockingbird.stream

import eu.timepit.fs2cron.cron4s.Cron4sScheduler
import eu.timepit.refined.*
import eu.timepit.refined.numeric.*
import fs2.Stream
import tofu.logging.Logging
import tofu.logging.impl.ZUniversalLogging
import zio.interop.catz.*

import ru.tinkoff.tcb.criteria.*
import ru.tinkoff.tcb.criteria.Typed.*
import ru.tinkoff.tcb.mockingbird.dal.GrpcMethodDescriptionDAO
import ru.tinkoff.tcb.mockingbird.dal.GrpcStubDAO
import ru.tinkoff.tcb.mockingbird.dal.HttpStubDAO
import ru.tinkoff.tcb.mockingbird.dal.ScenarioDAO
import ru.tinkoff.tcb.mockingbird.model.GrpcMethodDescription
import ru.tinkoff.tcb.mockingbird.model.GrpcStub
import ru.tinkoff.tcb.mockingbird.model.HttpStub
import ru.tinkoff.tcb.mockingbird.model.Scenario
import ru.tinkoff.tcb.mockingbird.model.Scope

final class EphemeralCleaner(
    stubDAO: HttpStubDAO[Task],
    scenarioDAO: ScenarioDAO[Task],
    grpcStubDAO: GrpcStubDAO[Task],
    grpcMethodDescriptionDAO: GrpcMethodDescriptionDAO[Task]
) {
  private val log: Logging[UIO] = new ZUniversalLogging(this.getClass.getName)

  private val cronScheduler = Cron4sScheduler.systemDefault[Task]

  private val trigger = cronScheduler.awakeEvery(midnight)

  private val secondsInDay = 60 * 60 * 24

  private val cleanup = Stream.eval[Task, Long] {
    for {
      current <- ZIO.clock.flatMap(_.instant)
      threshold = current.minusSeconds(secondsInDay)
      deleted <- stubDAO.delete(
        prop[HttpStub](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown) && prop[HttpStub](_.created) < threshold
      )
      _ <- log.info("Purging expired stubs: {} deleted", deleted)
      deleted2 <- scenarioDAO.delete(
        prop[Scenario](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown) && prop[Scenario](_.created) < threshold
      )
      _ <- log.info("Purging expired scenarios: {} deleted", deleted2)
      methodDescriptions <- grpcMethodDescriptionDAO.findChunk(
        prop[GrpcMethodDescription](_.scope).in[Scope](Scope.Ephemeral, Scope.Countdown),
        0,
        Int.MaxValue
      )
      deleted3 <- grpcStubDAO.delete(
        prop[GrpcStub](_.methodDescriptionId).in(methodDescriptions.map(_.id)) && prop[GrpcStub](_.created) < threshold
      )
      _ <- log.info("Purging expired grpc stubs: {} deleted", deleted3)
      deleted4 <- stubDAO.delete(
        prop[HttpStub](_.scope) === Scope.Countdown.asInstanceOf[Scope] && prop[HttpStub](_.times) <= Option(
          refineMV[NonNegative](0)
        )
      )
      _ <- log.info("Purging countdown stubs: {} deleted", deleted4)
      deleted5 <- scenarioDAO.delete(
        prop[Scenario](_.scope) === Scope.Countdown.asInstanceOf[Scope] && prop[Scenario](_.times) <= Option(
          refineMV[NonNegative](0)
        )
      )
      _ <- log.info("Purging countdown scenarios: {} deleted", deleted5)
      deleted6 <- grpcStubDAO.delete(
        prop[GrpcStub](_.methodDescriptionId).in(methodDescriptions.filter(_.scope == Scope.Countdown).map(_.id)) &&
          prop[GrpcStub](_.times) <= Option(refineMV[NonNegative](0))
      )
      _ <- log.info("Purging countdown grpc stubs: {} deleted", deleted6)
    } yield deleted
  }

  private val stream = trigger >> cleanup

  def run: Task[Unit] = stream.compile.drain
}

object EphemeralCleaner {
  val live = ZLayer {
    for {
      hsd  <- ZIO.service[HttpStubDAO[Task]]
      sd   <- ZIO.service[ScenarioDAO[Task]]
      gsd  <- ZIO.service[GrpcStubDAO[Task]]
      gmdd <- ZIO.service[GrpcMethodDescriptionDAO[Task]]
    } yield new EphemeralCleaner(hsd, sd, gsd, gmdd)
  }
}
