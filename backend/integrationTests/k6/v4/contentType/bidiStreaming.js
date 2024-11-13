import encoding from 'k6/encoding';
import http from 'k6/http';
import grpc from 'k6/net/grpc';
import { check, sleep } from 'k6';

import { serviceName } from './scenario.js'
import { httpHost, httpUri, httpOptions, grpcHost } from './scenario.js'
import { setup as scenarioSetup, teardown as scenarioTeardown } from './scenario.js'

const methodName = 'market_data.OTCMarketDataService/PricesBidi'

const grpcClient = new grpc.Client();
grpcClient.load(['definitions'], 'test_service.proto');

const protoFile = open('./definitions/test_service.proto');
const base64Proto = encoding.b64encode(protoFile);

export function setup() {
  scenarioSetup()
}

export default function() {

  const methodDescriptionData = {
    id: "bidi-streaming-method-description",
    description: "k6 testing",
    service: serviceName,
    methodName: methodName,
    connectionType: "BIDI_STREAMING",
    proxyUrl: null,
    requestClass: "PricesRequest",
    responseClass: "PricesResponse",
    requestCodecs: base64Proto,
    responseCodecs: base64Proto
  }
  const methodDescriptionRes = http.post(httpUri('/v4/grpcMethodDescription'), JSON.stringify(methodDescriptionData), httpOptions)
  check(methodDescriptionRes, { 'create method description - status is OK': (r) => r.status === 200 })

  const stubData = {
    methodDescriptionId: "bidi-streaming-method-description",
    scope: "countdown",
    times: 2,
    name: "bidirectional streaming v4 countdown stub",
    response: {
      "data": {
        "code": "OK",
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

  const grpcReq = [
    { instrument_id: 'instrument_1', instrument_id_kind: 'ID_1' },
    { instrument_id: 'instrument_2', instrument_id_kind: 'ID_2' },
  ]

  const stream = new grpc.Stream(grpcClient, methodName)
  let inputStreamIndex = 0

  stream.on('data', (data) => {
    console.log('Stream#1 Value: ' + JSON.stringify(data));
    check(data, {
       'response stream element - validation is OK': (d) => d && inputStreamIndex < grpcReq.length &&
         d.instrumentId === grpcReq[inputStreamIndex].instrument_id && d.trackingId === grpcReq[inputStreamIndex].instrument_id_kind,
    });
    inputStreamIndex += 1
  })

  stream.on('error', (err) => {
    console.log('Stream#1 Error: ' + JSON.stringify(err));
    check(err, { 'response stream element - should be empty': (e) => false })
  })

  stream.on('end', () => {
    console.log('All done');
  })

  for (let i in grpcReq) {
    stream.write(grpcReq[i])
    sleep(0.1)
  }

  stream.end();


  const stream2 = new grpc.Stream(grpcClient, methodName)

  stream2.on('data', (data) => {
    console.log('Stream#2 Value: ' + JSON.stringify(data));
    check(data, { 'recall response stream element - should be empty': (d) => false })
  })

  stream2.on('error', (err) => {
    console.log('Stream#2 Error: ' + JSON.stringify(err));
    check(err, {
        'recall response stream element - status is Internal': (e) => e && e.code == 13 &&
          e.message === `Can't find any stub for ${methodName}`,
    })
  })

  stream2.on('end', () => {
    console.log('All done');
  })

  for (let i in grpcReq) {
    stream2.write(grpcReq[i])
  }

  stream2.end();

}

export function teardown() {
  scenarioTeardown()
  grpcClient.close()
}

function makeStream(grpcClient, methodName, onData) {
  const stream = new Stream(grpcClient, methodName)

  stream.on('data', onData)

  stream.on('error', (err) => {
    console.log('Stream Error: ' + JSON.stringify(err));
  })

  stream.on('end', () => {
    console.log('All done');
  })

  return stream
}
