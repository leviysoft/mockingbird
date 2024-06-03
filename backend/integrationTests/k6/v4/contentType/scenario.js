import http from 'k6/http';
import { check, sleep } from 'k6';
import { default as unaryV4Test } from './unary.js';
import { default as serverStreamingV4Test } from './serverStreaming.js';
import { default as clientStreamingV4Test } from './clientStreaming.js';
import { default as bidiStreamingV4Test } from './bidiStreaming.js';

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
    unaryV4Scenario: {
      executor: 'per-vu-iterations',
      exec: 'unaryV4Scenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'unary' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    serverStreamingV4Scenario: {
      executor: 'per-vu-iterations',
      exec: 'serverStreamingV4Scenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'server_streaming' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    clientStreamingV4Scenario: {
      executor: 'per-vu-iterations',
      exec: 'clientStreamingV4Scenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'client_streaming' },

      vus: 1,
      iterations: 1,
      maxDuration: '5s',
    },
    bidiStreamingV4Scenario: {
      executor: 'per-vu-iterations',
      exec: 'bidiStreamingV4Scenario',

      startTime: '0s',
      gracefulStop: '2s',
      tags: { connectionType: 'bidi_streaming' },

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

export function unaryV4Scenario() {
  unaryV4Test()
}

export function serverStreamingV4Scenario() {
  serverStreamingV4Test()
}

export function clientStreamingV4Scenario() {
  clientStreamingV4Test()
}

export function bidiStreamingV4Scenario() {
  bidiStreamingV4Test()
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

}
