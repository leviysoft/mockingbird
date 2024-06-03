import http from 'k6/http';

import { default as ephemeralTest, closeGrpcClient as closeEphemeralGrpcClient } from './ephemeral.js';
import { default as persistentTest, closeGrpcClient as closePersistentGrpcClient } from './persistent.js';
import { default as countdownTest, closeGrpcClient as closeCountdownGrpcClient } from './countdown.js';

export const httpHost = 'http://localhost:8228/api/internal/mockingbird';
export const grpcHost = 'localhost:9000';

export function httpUri(path) {
  return httpHost + path
}

export const httpOptions = {
  headers: { 'Content-Type': 'application/json' }
}

export const serviceName = 'beta'

export const options = {
  scenarios: {
    ephemeralScenario: {
      executor: 'per-vu-iterations',
      exec: 'ephemeralScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'unary', responseMode: 'fill' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    persistentScenario: {
      executor: 'per-vu-iterations',
      exec: 'persistentScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'unary', responseMode: 'fill' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    countdownScenario: {
      executor: 'per-vu-iterations',
      exec: 'countdownScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'unary', responseMode: 'fill' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
  },
};

export function setup() {
  const serviceData = { name: serviceName, suffix: serviceName }
  const serviceRes = http.post(httpUri('/v2/service'), JSON.stringify(serviceData), httpOptions)
}

export function ephemeralScenario() {
  ephemeralTest()
}

export function persistentScenario() {
  persistentTest()
}

export function countdownScenario() {
  countdownTest()
}

export function teardown() {

// delete all method descriptions and stubs for service
  let methodDescriptionsRes = http.get(httpUri(`/v4/grpcMethodDescription?service=${serviceName}`))
  let methodDescriptionIds = methodDescriptionsRes.json("#.id")
  console.info(`methodDescriptionIds=${methodDescriptionIds}`)

  for (let i in methodDescriptionIds) {
    let methodDescriptionId = methodDescriptionIds[i]
    let grpcStubsRes = http.get(httpUri(`/v4/grpcStub?query=${methodDescriptionId}`))
    let grpcStubIds = grpcStubsRes.json("#.id")
    console.info(`Alive grpcStubIds=${grpcStubIds}`)

    for (let i in grpcStubIds) {
      let stubId = grpcStubIds[i]
      let grpcStubRes = http.del(httpUri(`/v2/grpcStub/${stubId}`))
      console.info(`del stub: id=${stubId}; status=${grpcStubRes.status}`)
    }

    let methodDescriptionRes = http.del(httpUri(`/v4/grpcMethodDescription/${methodDescriptionId}`))
    console.info(`del methodDescription: id=${methodDescriptionId}; status=${methodDescriptionRes.status}`)
  }

  closeEphemeralGrpcClient()
  closePersistentGrpcClient()
  closeCountdownGrpcClient()
}
