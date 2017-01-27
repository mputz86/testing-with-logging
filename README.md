
# Testing with Logging Framework

This framework should demonstrate how testing with logging can be realized.

In the main project is the framework (`com.innoq.framework`) as well as tests for the docker-example (`com.innoq.tests`).
The demo application can be found in the 'docker-example' folder.

This framework is part of an article describing the possibility for testing with logging

[TODO link article somehow].

Furthermore are the [Logging Guidelines](https://github.com/mputz86/logging-guidelines) part of this description for testing with logging.


## Running the tests

The tests can be run by calling

```
./build-and-run.sh
```

in the main directory. This builds all the docker images in 'docker-example', builds the test framework and tests as well as runs the tests (may take up to 12 minutes!).


## Interesting parts

* Starting from the tests like `ShopTest` are especially the `waitFor` statements interesting.
The implementation can be found in the `LogTesting` class.

* Also very helpful is the interception mechanism used in `ShopApiTesting.fakeRating(..)`.
The implementation for methods like `setInterception` and `removeInterception` can be found in `HttpInterceptionTesting`.

* Furthermore is the `InfrastructureTesting` class interesting if, for example, kubernetes should be used as infrastructure, instead of docker.


## Docker-Example setup

The docker-example consists of:

* Two services (user and shop service) whereas
  * the shop server calls the user service for debiting the users balance and
  * calls an external API for ratings for an item.
* A test-server which is used to mock the external API call from the shop service.
* An ELK Stack which receives all log messages from the services and the test server.
* A redis container which receives receives all messages from the ELK Stack and publishes them on the log channel (the test framework is subscribed to the log channel)
* A docker-compose.yml file which connects everything for the test


## Example Test Trace

The following is an example of a test trace. The trace contains log statements from the services as well as statements of the log framework.

```
10:14:15.789 INFO  akka.event.slf4j.Slf4jLogger - Slf4jLogger started
10:14:15.902 INFO  Test - Using docker infrastructure
10:14:15.909 INFO  Test - Stopping and removing all containers
10:14:42.480 INFO  Test - Starting infrastructure containers redis elk test_server
10:14:47.818 INFO  Test - Creating application containers user_service shop_service
10:14:48.714 INFO  Test - Waiting 2000 milliseconds for infrastructure to be started
10:14:50.724 INFO  akka.event.slf4j.Slf4jLogger - Slf4jLogger started
10:14:50.729 INFO  Test - Starting redis log message subscriber on localhost:6379
10:14:50.731 INFO  Test - Verbose logging
10:14:50.767 INFO  c.i.f.impl.RedisLogMessageSubscriber - Connect to localhost/127.0.0.1:6379
10:14:50.808 INFO  c.i.f.impl.RedisLogMessageSubscriber - Connected to localhost/127.0.0.1:6379
10:14:51.322 INFO  Test - Starting application containers user_service shop_service
10:14:53.426 INFO  Test - Adding startup delay of 40000ms to first wait for statement (timeout factor=2.0)
10:14:53.431 INFO  Test - >> Waiting 100000ms for: INFO/shop: Starting shop controller
10:15:20.818 INFO  Test - INFO/test_server: Starting interception controller
10:15:20.820 INFO  Test - INFO/test_server: Starting mock controller
10:15:23.714 INFO  Test - INFO/user: Starting user controller
10:15:25.430 INFO  Test - INFO/shop: Starting shop controller
10:15:25.433 INFO  Test - << Continuing test after: time=32002ms, message=INFO/shop: Starting shop controller
10:15:25.436 INFO  Test - Requesting and waiting: url=http://localhost:8082/shop/items/CoolItem, method=GET, content=
10:15:26.294 INFO  Test - << GET http://localhost:8082/shop/items/CoolItem: status=200, body={"name":"CoolItem","ratings":[]}
10:15:26.472 INFO  Test - >> Waiting 20000ms for: DEBUG/shop: Ignoring failed external ratings request: .*
10:15:26.714 INFO  Test - DEBUG/shop: Processing get item request: name=CoolItem
10:15:26.715 INFO  Test - DEBUG/shop: Getting item details: name=CoolItem
10:15:26.716 INFO  Test - DEBUG/test_server: Processing request: GET /ratings/CoolItem
10:15:26.718 INFO  Test - DEBUG/test_server: Could not find GET /ratings/CoolItem in
10:15:26.719 INFO  Test - INFO/test_server: Successful request GET /ratings/CoolItem: Response(500,None,List())
10:15:26.721 INFO  Test - DEBUG/shop: Ignoring failed external ratings request: name=CoolItem, ignored exception=java.lang.Exception: Failed to get ratings: name=CoolItem, status code=500, response=
10:15:26.722 INFO  Test - << Continuing test after: time=250ms, message=DEBUG/shop: Ignoring failed external ratings request: name=CoolItem, ignored exception=java.lang.Exception: Failed to get ratings: name=CoolItem, status code=500, response=
10:15:26.723 INFO  Test - Stopping application containers user_service shop_service
10:15:48.648 INFO  Test - Removing interceptions
10:15:48.673 INFO  Test - Stopping and removing all containers
10:15:48.683 INFO  c.i.f.impl.RedisLogMessageSubscriber - RedisWorkerIO stop
10:16:05.280 INFO  Test - Stopping and removing all containers
```

