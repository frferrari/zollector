package com.zollector.marketplace.repositories

import com.zollector.marketplace.domain.data.User
import zio.*
import zio.test.*

import javax.sql.DataSource
import com.zollector.marketplace.http.requests.RegisterUserRequest
import com.zollector.marketplace.repositories.*

import java.time.Instant

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/users.sql"

  private val user1 = User(
    id = -1L,
    nickname = "boblazar51",
    email = "boblazar@area51.com",
    firstName = "Bob",
    lastName = "Lazar",
    hashedPassword = "mypassword",
    createdAt = Instant.now()
  )

  private val user2 = User(
    id = -1,
    nickname = "michiokaku",
    email = "michiokaky@physics.com",
    firstName = "Michio",
    lastName = "Kaku",
    hashedPassword = "hispassword",
    createdAt = Instant.now()
  )

  private val updatedUser1 = User(
    id = -1L,
    nickname = "boblazarfiftyone",
    email = "boblazar@areafiftyone.com",
    firstName = "Boby",
    lastName = "Lazaro",
    hashedPassword = "Lazarus",
    createdAt = Instant.now()
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserRepositorySpec")(
      test("create a user") {
        for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(user1)
        } yield assertTrue(
          user.id == 1L
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
      test("update a user") {
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
      test("delete a user that exists, and fails on one that doesn't exist") {
        for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(user1)

          // Should return true when successfully deleting a user
          isUserDeleted <- repo.delete(user.id)

          // Should return false when it fails to delete a user
          isUnknownUserDeleted <- repo.delete(user.id + 1L)

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
