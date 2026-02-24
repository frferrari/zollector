# Description

A project based on the zio-rite-of-passage course from RockTheJVM

# Manual testing

- Register a User

```http post localhost:8080/users nickname='boblazar' email='admin@zollector.com' password='bobPassword' firstName='bob' lastName='lazar'```

- Login a User

```http post localhost:8080/users/login email='admin@zollector.com' password='bobPassword'```

- Delete a User ; the token provided below is the one produced by the command above

```http delete localhost:8080/users email='admin@zollector.com' password='bobPassword' 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ6b2xsZWN0b3IuY29tIiwiaWF0IjoxNzcxNDI4MjI3LCJleHAiOjE3NzIyOTIyMjcsInN1YiI6IjEiLCJlbWFpbCI6ImFkbWluQHpvbGxlY3Rvci5jb20ifQ.9zGV2u6gJ9o-X_qRo7ulbvidG6S6kyoSJfW8cscY48_h1gYK1HOqNoFLDQcUk0FfDXtfC-kwJBsfLPbAYOE8cQ'```

- Insert data in the db

```
insert into collections values (gen_random_uuid(), gen_random_uuid(), 1, 1, 'Finland 1960 1990', 'Stamps from Finland 1960 to 1990', 1960, 1990, 'finland-1960-1990', null, now(), null);
insert into collections values (gen_random_uuid(), gen_random_uuid(), 1, 1, 'Norway 1950 2000', 'Stamps from Norway 1950 to 2000', 1950, 2000, 'norway-1950-2000', null, now(), null);
```

- Load category and category_translations

```
docker exec -i postgres_zollector psql -U zollector -d zollector_dev < ../dumps/category.sql
docker exec -i postgres_zollector psql -U zollector -d zollector_dev < ../dumps/category_translations.sql
```