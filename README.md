# Manual testing

- Register a User

http post localhost:8080/users nickname='boblazar' email='admin@zollector.com' password='bobPassword' firstName='bob'
lastName='lazar'

- Login a User

http post localhost:8080/users/login email='admin@zollector.com' password='bobPassword'

- Delete a User ; the token provided below is the one produced by the command above

http delete localhost:8080/users email='admin@zollector.com' password='bobPassword' 'Authorization: Bearer
eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ6b2xsZWN0b3IuY29tIiwiaWF0IjoxNzcxNDI4MjI3LCJleHAiOjE3NzIyOTIyMjcsInN1YiI6IjEiLCJlbWFpbCI6ImFkbWluQHpvbGxlY3Rvci5jb20ifQ.9zGV2u6gJ9o-X_qRo7ulbvidG6S6kyoSJfW8cscY48_h1gYK1HOqNoFLDQcUk0FfDXtfC-kwJBsfLPbAYOE8cQ'