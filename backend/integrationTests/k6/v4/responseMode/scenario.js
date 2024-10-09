import http from 'k6/http';

import { default as fillStreamTest, closeGrpcClient as closeFillStreamGrpcClient } from './fillStream.js';
import { default as repeatTest, closeGrpcClient as closeRepeatGrpcClient } from './repeat.js';
import { default as noBodyTest, closeGrpcClient as closeNoBodyGrpcClient } from './noBody.js';
import { default as streamOutputUnaryConnectionTest, closeGrpcClient as closeStreamOutputUnaryGrpcClient } from './streamOutputForUnaryConnection.js';

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
    fillStreamScenario: {
      executor: 'per-vu-iterations',
      exec: 'fillStreamScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'server_streaming', responseMode: 'fill_stream' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    repeatScenario: {
      executor: 'per-vu-iterations',
      exec: 'repeatScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'server_streaming', responseMode: 'repeat' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    noBodyScenario: {
      executor: 'per-vu-iterations',
      exec: 'noBodyScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'server_streaming', responseMode: 'no_body' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    streamOutputUnaryConnectionScenario: {
      executor: 'per-vu-iterations',
      exec: 'streamOutputUnaryConnectionScenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'server_streaming', responseMode: 'no_body' },

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

export function fillStreamScenario() {
  fillStreamTest()
}

export function repeatScenario() {
  repeatTest()
}

export function noBodyScenario() {
  noBodyTest()
}

export function streamOutputUnaryConnectionScenario() {
  streamOutputUnaryConnectionTest()
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

  closeFillStreamGrpcClient()
  closeRepeatGrpcClient()
  closeNoBodyGrpcClient()
  closeStreamOutputUnaryGrpcClient()
}
