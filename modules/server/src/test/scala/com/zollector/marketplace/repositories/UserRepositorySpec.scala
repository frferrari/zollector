package com.zollector.marketplace.repositories

import com.zollector.marketplace.domain.data.User
import zio.*
import zio.test.*

import javax.sql.DataSource
import com.zollector.marketplace.http.requests.RegisterUserRequest
import com.zollector.marketplace.repositories.*

import java.time.Instant
import com.zollector.marketplace.domain.data.ValueObjects.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/users.sql"

  private val user1 = User(
    id = UserId.random,
    nickname = "boblazar51",
    email = Email("boblazar@area51.com"),
    firstName = "Bob",
    lastName = "Lazar",
    hashedPassword = HashedPassword("mypassword"),
    createdAt = Instant.now()
  )

  private val user2 = User(
    id = UserId.random,
    nickname = "michiokaku",
    email = Email("michiokaky@physics.com"),
    firstName = "Michio",
    lastName = "Kaku",
    hashedPassword = HashedPassword("hispassword"),
    createdAt = Instant.now()
  )

  private val updatedUser1 = User(
    id = user1.id,
    nickname = "boblazarfiftyone",
    email = Email("boblazar@areafiftyone.com"),
    firstName = "Boby",
    lastName = "Lazaro",
    hashedPassword = HashedPassword("Lazarus"),
    createdAt = Instant.now()
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserRepositorySpec")(
      test("create a user") {
        for {
          repo        <- ZIO.service[UserRepository]
          userCreated <- repo.create(user1)
          fetchedUser <- repo.getByEmail(user1.email).someOrFail("Could not fetch user after creation")
        } yield assertTrue(
          userCreated.id == user1.id &&
            userCreated.nickname == user1.nickname &&
            userCreated.email == user1.email &&
            userCreated.firstName == user1.firstName &&
            userCreated.lastName == user1.lastName &&
            fetchedUser.id == user1.id &&
            fetchedUser.nickname == user1.nickname &&
            fetchedUser.email == user1.email &&
            fetchedUser.firstName == user1.firstName &&
            fetchedUser.lastName == user1.lastName
        )
      },
      test("get a user by id and email and nickname") {
        for {
          repo           <- ZIO.service[UserRepository]
          user           <- repo.create(user1)
          userById       <- repo.getById(user.id)
          userByEmail    <- repo.getByEmail(user.email)
          userByNickname <- repo.getByNickname(user.nickname)
        } yield assertTrue(
          userById.contains(user) &&
            userByEmail.contains(user) &&
            userByNickname.contains(user)
        )
      },
      test("updateById a user") {
        for {
          repo        <- ZIO.service[UserRepository]
          createdUser <- repo.create(user1)
          updatedUser <- repo.update(createdUser.id, updatedUser1)
        } yield assertTrue(
          createdUser.id == updatedUser.id &&
            updatedUser.nickname == updatedUser1.nickname &&
            updatedUser.email == updatedUser1.email &&
            updatedUser.firstName == updatedUser1.firstName &&
            updatedUser.lastName == updatedUser1.lastName &&
            updatedUser.hashedPassword == updatedUser1.hashedPassword
        )
      },
      test("deleteById a user that exists, and fails on one that doesn't exist") {
        for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(user1)

          // Should return true when successfully deleting a user
          isUserDeleted <- repo.delete(user.id)

          // Should return false when it fails to deleteById a user
          isUnknownUserDeleted <- repo.delete(UserId.random)

          // Check that the user no longer exists in the DB
          checkById <- repo.getById(user.id)
        } yield assertTrue(
          isUserDeleted &&
            !isUnknownUserDeleted &&
            checkById.isEmpty
        )
      }
    ).provide(UserRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
