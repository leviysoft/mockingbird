import encoding from 'k6/encoding';
import http from 'k6/http';
import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

import { serviceName } from './scenario.js'
import { httpHost, httpUri, httpOptions, grpcHost } from './scenario.js'
import { setup as scenarioSetup, teardown as scenarioTeardown } from './scenario.js'

const methodName = 'market_data.OTCMarketDataService/Persistent'

const grpcClient = new grpc.Client();
grpcClient.load(['definitions'], 'test_service.proto');

const protoFile = open('./definitions/test_service.proto');
const base64Proto = encoding.b64encode(protoFile);

export function setup() {
  scenarioSetup()
}

export default function() {

  const methodDescriptionData = {
    id: "unary-persistent",
    description: "k6 testing scope",
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
    methodDescriptionId: "unary-persistent",
    scope: "persistent",
    name: "persistent stub",
    response: {
      "data": {
        "instrument_id": "${req.instrument_id}",
        "tracking_id": "${req.instrument_id_kind}"
      },
      "mode":"fill"
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

  for (let i = 0; i < 5; i++) {
    invokeAndCheck(grpcReq)
  }

}

function invokeAndCheck(grpcReq) {
  const response = grpcClient.invoke(methodName, grpcReq)
  check(response, {
      'call grpc stub - status is OK': (r) => r && r.status === grpc.StatusOK,
      'call grpc stub - check response message': (r) => r.message &&
        r.message.instrumentId === grpcReq.instrument_id && r.message.trackingId === grpcReq.instrument_id_kind,
  })
}

export function teardown() {
  scenarioTeardown()
}

export function closeGrpcClient() {
  grpcClient.close()
}

