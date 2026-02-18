package com.zollector.marketplace.services

import zio.*
import zio.test.*
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.repositories.UserRepository
import com.zollector.marketplace.services.*

object UserServiceSpec extends ZIOSpecDefault {

  val registerBobRequest =
    RegisterUserRequest("boblazar", "admin@zollector.com", "bobPassword", "bob", "lazar")

  val updateBobPasswordRequest =
    UpdatePasswordRequest(registerBobRequest.email, registerBobRequest.password, "newPassword")

  val deleteBobAccountRequest =
    DeleteUserRequest(registerBobRequest.email, registerBobRequest.password)

  val loginRequest =
    LoginRequest(registerBobRequest.email, registerBobRequest.password)

  val bobLazar = User(
    1L,
    registerBobRequest.nickname,
    registerBobRequest.email,
    registerBobRequest.firstName,
    registerBobRequest.lastName,
    "1000:957CFE57B3A3C7FE1888AA9E00FB2E05E385EE6330A89374:7D59DE2824017BE7ED869B50D41F1A8F3A32AF05E82D2E00",
    java.time.Instant.now()
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](bobLazar.id -> bobLazar)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def getByNickname(nickname: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.nickname == nickname))

      override def update(id: Long, user: User): Task[User] =
        ZIO.attempt {
          val currentUser = db(id)
          db += (id -> user)
          user
        }

      override def delete(id: Long): Task[Boolean] =
        ZIO.attempt {
          val user = db(id)
          db -= id
          true
        }
    }
  }

  val stubJwtLayer = ZLayer.succeed({
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "A TOKEN", Long.MaxValue))

      override def verityToken(token: String): Task[UserID] =
        ZIO.succeed(UserID(bobLazar.id, bobLazar.email))
    }
  })

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(registerBobRequest)
          valid   <- service.verifyPassword(bobLazar.email, registerBobRequest.password)
        } yield assertTrue(valid && user.email == bobLazar.email)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(bobLazar.email, registerBobRequest.password)
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(bobLazar.email, "wrongpassword")
        } yield assertTrue(!valid)
      },
      test("invalidate non existent user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("unknownuser@gmail.com", "wrongpassword")
        } yield assertTrue(!valid)
      },
      test("update password") {
        val newPassword = "newPassword"
        for {
          service  <- ZIO.service[UserService]
          newUser  <- service.updatePassword(updateBobPasswordRequest)
          oldValid <- service.verifyPassword(bobLazar.email, registerBobRequest.password)
          newValid <- service.verifyPassword(bobLazar.email, newPassword)
        } yield assertTrue(!oldValid && newValid)
      },
      test("delete with non existent user should fail") {
        for {
          service <- ZIO.service[UserService]
          error <- service
            .deleteUser(DeleteUserRequest("unknownuser@gmail.com", registerBobRequest.password))
            .flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("delete with incorrect credentials should fail") {
        for {
          service <- ZIO.service[UserService]
          error   <- service.deleteUser(DeleteUserRequest(bobLazar.email, "wrongpassword")).flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("delete user") {
        for {
          service <- ZIO.service[UserService]
          result  <- service.deleteUser(deleteBobAccountRequest)
        } yield assertTrue(result)
      }
    ).provide(UserServiceLive.layer, stubJwtLayer, stubRepoLayer)
}
