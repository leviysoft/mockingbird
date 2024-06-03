# How to test with k6

k6 is used for integration testing. 
At first start mockingbird app, then set up `httpHost` and `grpcHost` variables, after that run tests.

### Shell commands

- To create new test use `k6 new fileName.js`

- To run test script use `k6 run filename.js`

- To group several tests in a single run use scenarios (e.g. see file `scenario.js`).
To run scenario use `k6 run scenario.js`

### Links

- [Running k6](https://grafana.com/docs/k6/latest/get-started/running-k6/)
- [Javascript API](https://grafana.com/docs/k6/latest/javascript-api/)