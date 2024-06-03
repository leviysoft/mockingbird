import encoding from 'k6/encoding';
import http from 'k6/http';
import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

import { serviceName } from './scenario.js'
import { httpHost, httpUri, httpOptions, grpcHost } from './scenario.js'
import { setup as scenarioSetup, teardown as scenarioTeardown } from './scenario.js'

const methodName = 'market_data.OTCMarketDataService/Unary'

const grpcClient = new grpc.Client();
grpcClient.load(['definitions'], 'test_service.proto');

const protoFile = open('./definitions/test_service.proto');
const base64Proto = encoding.b64encode(protoFile);

export function setup() {
  scenarioSetup()
}

export default function() {

  const methodDescriptionData = {
    id: "unary-response-mode-fill-stream",
    description: "k6 testing response mode",
    service: serviceName,
    methodName: methodName,
    connectionType: "UNARY",
    proxyUrl: null,
    requestClass: "PricesRequest",
    responseClass: "PricesResponse",
    requestCodecs: base64Proto,
    responseCodecs: base64Proto
  }
  const methodDescriptionRes = http.post(httpUri('/v4/grpcMethodDescription'), JSON.stringify(methodDescriptionData), httpOptions)
  check(methodDescriptionRes, { 'create method description - status is OK': (r) => r.status === 200 })

  const stubData = {
    methodDescriptionId: "unary-response-mode-fill-stream",
    scope: "countdown",
    times: 1,
    name: "stub with fill stream response mode",
    response: {
      "data": [
        {
          "instrument_id": "${req.instrument_id}",
          "tracking_id": "${req.instrument_id_kind}"
        },
        {
          "instrument_id": "${req.instrument_id}",
          "tracking_id": "${req.instrument_id_kind}"
        }
      ],
      "streamDelay": "1s",
      "mode":"fill_stream"
    },
    requestPredicates:{},
    state: null,
    seed: null,
    persist: null,
    labels: []
  }
  const stubRes = http.post(httpUri('/v4/grpcStub'), JSON.stringify(stubData), httpOptions)
  check(stubRes, { 'create stub - status is OK': (r) => r.status === 200 })

  grpcClient.connect(grpcHost, { plaintext: true })

  const grpcReq = { instrument_id: 'instrument_1', instrument_id_kind: 'ID_1' }

  const response = grpcClient.invoke(methodName, grpcReq)
  check(response, {
      'call grpc stub - status is Internal': (r) => r && r.status === grpc.StatusInternal,
      'call grpc stub - check response message': (r) => (r) => r.error &&
      r.error.message === 'Found stream response for unary output',
  })

}

export function teardown() {
  scenarioTeardown()
}

export function closeGrpcClient() {
  grpcClient.close()
}
