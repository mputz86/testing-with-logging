
# User Service

Example service for testing-with-logging.

## Building docker image

`sbt docker`

## Notes

The important file with the endpoints is  `com.company.controller.UserController`:

* Creating new user
```
POST /users {"name":"Hans","password":"1234"}
```

* Getting user by id
```
GET /users/1
```

* Crediting balance of user (here: amount 10)
```
POST /users/1/credit/10
```

* Debiting balance of user (here: amount 10), fails if user has not enough funds
```
POST /users/1/debit/10
```

