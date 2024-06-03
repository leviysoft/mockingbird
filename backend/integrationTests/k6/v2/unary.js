import encoding from 'k6/encoding';
import http from 'k6/http';
import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

import { serviceName } from './scenario.js'
import { httpHost, httpUri, httpOptions, grpcHost } from './scenario.js'
import { setup as scenarioSetup, teardown as scenarioTeardown } from './scenario.js'

const methodName = 'market_data.OTCMarketDataService/PricesUnary'

const grpcClient = new grpc.Client();
grpcClient.load(['definitions'], 'test_service.proto');

const protoFile = open('./definitions/test_service.proto');
const base64_proto = encoding.b64encode(protoFile);

export function setup() {
  scenarioSetup()
}

export default function() {

  const stubData = {
    name: "unary v2 countdown stub",
    labels: [],
    scope: "countdown",
    times: 1,
    methodName: methodName,
    requestClass: "PricesRequest",
    requestPredicates:{},
    responseClass: "PricesResponse",
    response: {
      "data": {
        "code": "OK",
        "instrument_id": "${req.instrument_id}",
        "tracking_id": "${req.instrument_id_kind}"
      },
      "mode":"fill"
    },
    state: null,
    seed: null,
    service: serviceName,
    requestCodecs: base64_proto,
    responseCodecs: base64_proto
  }
  const stubRes = http.post(httpUri('/v2/grpcStub'), JSON.stringify(stubData), httpOptions)
  check(stubRes, { 'create stub status': (r) => r.status === 200 })

  grpcClient.connect(grpcHost, { plaintext: true })

  const grpcReq = { instrument_id: 'instrument_1', instrument_id_kind: 'ID_1' }
  const response = grpcClient.invoke(methodName, grpcReq)
  const expectedMessage = { instrument_id: grpcReq.instrument_id, trackingId: grpcReq.instrument_id_kind, code : "OK" }
  check(response, {
      'call grpc stub - status is OK': (r) => r && r.status === grpc.StatusOK,
      'call grpc stub - check response message': (r) => r.message && r.message.code === "OK" &&
        r.message.instrumentId === grpcReq.instrument_id && r.message.trackingId === grpcReq.instrument_id_kind,
  })

  const response2 = grpcClient.invoke(methodName, grpcReq)
  check(response2, {
      'recall grpc stub - status is Internal': (r) => r && r.status === grpc.StatusInternal,
      'recall grpc stub - check response message': (r) => r.error && r.error.message === `Can't find any stub for ${methodName}`,
  })

}

export function teardown() {
  scenarioTeardown()
  grpcClient.close()
}
