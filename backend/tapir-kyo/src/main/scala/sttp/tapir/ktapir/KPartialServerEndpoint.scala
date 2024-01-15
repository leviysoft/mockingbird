package sttp.tapir.ktapir

import izumi.reflect.Tag
import kyo.*
import kyo.aborts.Aborts
import sttp.tapir.Endpoint
import sttp.tapir.EndpointErrorOutputVariantsOps
import sttp.tapir.EndpointInfo
import sttp.tapir.EndpointInfoOps
import sttp.tapir.EndpointInput
import sttp.tapir.EndpointInputsOps
import sttp.tapir.EndpointMetaOps
import sttp.tapir.EndpointOutput
import sttp.tapir.EndpointOutputsOps
import sttp.tapir.server.ServerEndpoint

/**
 * An endpoint with the security logic provided, and the main logic yet unspecified. See [[RichZEndpoint.zServerLogic]].
 *
 * The provided security part of the server logic transforms inputs of type `SECURITY_INPUT`, either to an error of type
 * `ERROR_OUTPUT`, or value of type `PRINCIPAL`.
 *
 * The part of the server logic which is not provided, will have to transform a tuple: `(PRINCIPAL, INPUT)` either into
 * an error, or a value of type `OUTPUT`.
 *
 * Inputs/outputs can be added to partial endpoints as to regular endpoints, however the shape of the error outputs is
 * fixed and cannot be changed. Hence, it's possible to create a base, secured input, and then specialise it with
 * inputs, outputs and logic as needed.
 *
 * @tparam SECURITY_INPUT
 *   Type of the security inputs, transformed into PRINCIPAL
 * @tparam PRINCIPAL
 *   Type of transformed security input.
 * @tparam INPUT
 *   Input parameter types.
 * @tparam ERROR_OUTPUT
 *   Error output parameter types.
 * @tparam OUTPUT
 *   Output parameter types.
 * @tparam C
 *   The capabilities that are required by this endpoint's inputs/outputs. `Any`, if no requirements.
 */
case class KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT: Tag, OUTPUT, -C](
    endpoint: Endpoint[SECURITY_INPUT, INPUT, ERROR_OUTPUT, OUTPUT, C],
    securityLogic: SECURITY_INPUT => PRINCIPAL < S with Aborts[ERROR_OUTPUT]
) extends EndpointInputsOps[SECURITY_INPUT, INPUT, ERROR_OUTPUT, OUTPUT, C]
  with EndpointOutputsOps[SECURITY_INPUT, INPUT, ERROR_OUTPUT, OUTPUT, C]
  with EndpointErrorOutputVariantsOps[SECURITY_INPUT, INPUT, ERROR_OUTPUT, OUTPUT, C]
  with EndpointInfoOps[C]
  with EndpointMetaOps { outer =>

  override type ThisType[-_R] = KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT, OUTPUT, _R]
  override type EndpointType[_A, _I, _E, _O, -_R] = KPartialServerEndpoint[S, _A, PRINCIPAL, _I, _E, _O, _R]

  override def securityInput: EndpointInput[SECURITY_INPUT] = endpoint.securityInput
  override def input: EndpointInput[INPUT] = endpoint.input
  override def errorOutput: EndpointOutput[ERROR_OUTPUT] = endpoint.errorOutput
  override def output: EndpointOutput[OUTPUT] = endpoint.output
  override def info: EndpointInfo = endpoint.info

  override private[tapir] def withInput[I2, C2](input: EndpointInput[I2]): KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, I2, ERROR_OUTPUT, OUTPUT, C with C2] =
    copy[S, SECURITY_INPUT, PRINCIPAL, I2, ERROR_OUTPUT, OUTPUT, C with C2](endpoint = endpoint.copy(input = input))

  override private[tapir] def withOutput[O2, C2](output: EndpointOutput[O2]): KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT, O2, C with C2] =
    copy[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT, O2, C with C2](endpoint = endpoint.copy(output = output))

  override private[tapir] def withErrorOutputVariant[E2, C2](output: EndpointOutput[E2], embedE: ERROR_OUTPUT => E2): KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, E2, OUTPUT, C with C2] = {
    import Flat.unsafe.unchecked
    val newSecurityLogic: SECURITY_INPUT => PRINCIPAL < S with Aborts[E2] =
      a => {
        val tmp = Aborts[ERROR_OUTPUT].run[PRINCIPAL, S with Aborts[ERROR_OUTPUT]](securityLogic(a))
        ???
      }

    KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, E2, OUTPUT, C with C2](
      endpoint = endpoint.copy(errorOutput = output),
      securityLogic = newSecurityLogic
    )
  }

  override private[tapir] def withInfo(info: EndpointInfo): KPartialServerEndpoint[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT, OUTPUT, C] =
    copy[S, SECURITY_INPUT, PRINCIPAL, INPUT, ERROR_OUTPUT, OUTPUT, C](endpoint = endpoint.copy(info = info))

  override protected def showType: String = "PartialServerEndpoint"
}
