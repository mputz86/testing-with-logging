
# Shop Service

Example service for testing-with-logging.

## Building docker image

`sbt docker`

## Notes

The important file with the endpoints is  `com.company.controller.ShopController`:

* Buy item 'CoolThing' with a price of 100 as user with id 10
```
POST /items/CoolThing/100 {"userId":"10"}
```

* Get an item 'CoolThing' with its ratings, if available. The ratings are returned from an external service which can be configured in "application.conf" `apis.external.ratings.url`
```
GET /items/CoolThing
```
