# Test Server

Test server for testing-with-logging.

## Building docker image

`sbt docker`

## Notes

* Setting a new interception
```
PUT /interceptions {"endpoint":{"path":"/ratings/CoolItem","method":"GET","headers":[]},"response":{"statusCode":200,"content":"[{\"name\":\"CoolItem\"}]","headers":[]}}
```

* The interception is then testable under the given path and method, i.e. with the setting from above:
```
GET /mocks/ratings/CoolItem
```

* Deleting all interceptions
```
DELETE /interceptions
```


## Todos

* make a more powerful API with being able to POST interceptions, aka sequences are possible ; currently a call to a mocked url does not remove the interception
