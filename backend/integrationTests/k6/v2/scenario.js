import http from 'k6/http';
import { check, sleep } from 'k6';
import { default as unaryV2Test } from './unary.js';

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
    unaryV2Scenario: {
      executor: 'per-vu-iterations',
      exec: 'unaryV2Scenario',

      startTime: '0s',
      gracefulStop: '5s',
      tags: { connectionType: 'unary' },

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

export function unaryV2Scenario() {
  unaryV2Test()
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
