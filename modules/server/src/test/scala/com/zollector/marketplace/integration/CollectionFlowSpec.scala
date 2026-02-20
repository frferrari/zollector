package com.zollector.marketplace.integration

import zio.*
import zio.test.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter

import javax.sql.DataSource

import com.zollector.marketplace.config.*
import com.zollector.marketplace.domain.data.UserToken
import com.zollector.marketplace.http.controllers.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.services.*

object CollectionFlowSpec extends ZIOSpecDefault with RepositorySpec with IntegrationSpec {

  override val initScript: String = "sql/integration.sql"

  case class TestContext(
      backendStub: SttpBackend[Task, Nothing],
      bobToken: UserToken,
      michioToken: UserToken
  )

  // User Requests
  val registerUserRequestBob = RegisterUserRequest(
    nickname = "boblazar",
    email = "bob@zollector.com",
    password = "bobPassword",
    firstName = "bob",
    lastName = "lazar"
  )
  private val loginUserRequestBob = LoginRequest(registerUserRequestBob.email, registerUserRequestBob.password)

  private val registerUserRequestMichio = RegisterUserRequest(
    nickname = "michiokaku",
    email = "michio@zollector.com",
    password = "michioPassword",
    firstName = "michio",
    lastName = "kaku"
  )
  private val loginUserRequestMichio = LoginRequest(registerUserRequestMichio.email, registerUserRequestMichio.password)

  // Collection Requests
  private val createBobCollectionRequest = CreateCollectionRequest(
    name = "Norway 1960 1990",
    description = "Stamps of Norway from 1960 to 1990",
    yearStart = Some(1960),
    yearEnd = Some(1990)
  )
  private val updateBobCollectionRequest = UpdateCollectionRequest(
    name = "Norway 1950 2000",
    description = "Stamps of Norway from 1950 to 2000",
    yearStart = Some(1950),
    yearEnd = Some(2000)
  )
  private val createMichioCollectionRequest = CreateCollectionRequest(
    name = "Finland 1950 1980",
    description = "Stamps of Finland from 1950 to 1980",
    yearStart = Some(1950),
    yearEnd = Some(1980)
  )
  private val updateMichioCollectionRequest = UpdateCollectionRequest(
    name = "Finland 1970 2010",
    description = "Stamps of Finland from 1970 to 2010",
    yearStart = Some(1970),
    yearEnd = Some(2010)
  )

  val emailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] = ZLayer.succeed(new EmailServiceProbe)

  private val testContextLayer: ZLayer[UserService & CollectionService & JWTService, Throwable, TestContext] =
    ZLayer.fromZIO {
      for {
        userController       <- UserController.makeZIO
        collectionController <- CollectionController.makeZIO
        backendStub = TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(collectionController.routes ++ userController.routes)
          .backend()
        _           <- registerUser(registerUserRequestBob, backendStub)
        _           <- registerUser(registerUserRequestMichio, backendStub)
        bobToken    <- loginUser(loginUserRequestBob, backendStub)
        michioToken <- loginUser(loginUserRequestMichio, backendStub)
      } yield TestContext(backendStub, bobToken, michioToken)
    }

  private val truncateCollections: ZIO[DataSource, Throwable, Unit] =
    ZIO.serviceWithZIO[DataSource] { ds =>
      ZIO.attempt {
        val conn = ds.getConnection
        try conn.createStatement().execute("TRUNCATE TABLE collections RESTART IDENTITY")
        finally conn.close()
      }.unit
    }

  override def spec: Spec[TestEnvironment & Scope, Any] = {
    suite("CollectionFlowSpec")(
      test("Create collections for different users") {
        for {
          ctx                   <- ZIO.service[TestContext]
          maybeBobCollection    <- createCollection(createBobCollectionRequest, ctx.bobToken, ctx.backendStub)
          maybeMichioCollection <- createCollection(createMichioCollectionRequest, ctx.michioToken, ctx.backendStub)
        } yield assertTrue(maybeBobCollection.nonEmpty && maybeMichioCollection.nonEmpty)
      },
      test("Update collection") {
        for {
          ctx <- ZIO.service[TestContext]

          // Create 1 collection per user
          bobCollection <- createCollection(createBobCollectionRequest, ctx.bobToken, ctx.backendStub)
            .someOrFail("bob Collection was not created")
          michioCollection <- createCollection(createMichioCollectionRequest, ctx.michioToken, ctx.backendStub)
            .someOrFail("michio Collection was not created")

          // Update bob's collection
          updatedBobCollectionResponse <- updateCollection(
            bobCollection.id,
            updateBobCollectionRequest,
            ctx.bobToken,
            ctx.backendStub
          )

          // Update michio's collection with bobUser (must fail)
          updatedMichioCollectionResponse <- updateCollection(
            michioCollection.id,
            updateMichioCollectionRequest,
            ctx.bobToken,
            ctx.backendStub
          )

          // Fetch bob's updated collection
          bobCollectionAfterUpdate <- getCollection(bobCollection.id, ctx.bobToken, ctx.backendStub)
            .someOrFail("could not fetch bobCollection after update")

          // Fetch michio's collection
          michioCollectionAfterUpdate <- getCollection(michioCollection.id, ctx.michioToken, ctx.backendStub)
            .someOrFail("could not fetch michioCollection after update")
        } yield assertTrue(
          updatedBobCollectionResponse.nonEmpty &&
            updatedMichioCollectionResponse.isEmpty &&
            bobCollectionAfterUpdate.id == bobCollection.id &&
            bobCollectionAfterUpdate.name == updateBobCollectionRequest.name &&
            bobCollectionAfterUpdate.description == updateBobCollectionRequest.description &&
            bobCollectionAfterUpdate.yearStart == updateBobCollectionRequest.yearStart &&
            bobCollectionAfterUpdate.yearEnd == updateBobCollectionRequest.yearEnd &&
            michioCollectionAfterUpdate.id == michioCollection.id &&
            michioCollectionAfterUpdate.name == createMichioCollectionRequest.name &&
            michioCollectionAfterUpdate.description == createMichioCollectionRequest.description &&
            michioCollectionAfterUpdate.yearStart == createMichioCollectionRequest.yearStart &&
            michioCollectionAfterUpdate.yearEnd == createMichioCollectionRequest.yearEnd
        )
      },
      test("Delete collection") {
        for {
          ctx <- ZIO.service[TestContext]

          // Create 1 collection per user
          bobCollection <- createCollection(createBobCollectionRequest, ctx.bobToken, ctx.backendStub)
            .someOrFail("bob Collection was not created")
          michioCollection <- createCollection(createMichioCollectionRequest, ctx.michioToken, ctx.backendStub)
            .someOrFail("michio Collection was not created")

          // Delete bob's collection
          bobCollectionDeleteResponse <- deleteCollection(bobCollection.id, ctx.bobToken, ctx.backendStub)

          // Try to delete michio's collection with bobUser (must fail)
          michioCollectionDeleteResponse <- deleteCollection(michioCollection.id, ctx.bobToken, ctx.backendStub)

          // Fetch the deleted collection (should be gone)
          maybeBobCollectionAfterDelete <- getCollection(bobCollection.id, ctx.bobToken, ctx.backendStub)

          // Fetch the non-deleted collection (should still be there)
          maybeMichioCollectionAfterDelete <- getCollection(michioCollection.id, ctx.michioToken, ctx.backendStub)
        } yield assertTrue(
          bobCollectionDeleteResponse.contains(true) &&
            michioCollectionDeleteResponse.contains(false) &&
            maybeBobCollectionAfterDelete.isEmpty &&
            maybeMichioCollectionAfterDelete.nonEmpty
        )
      }
    ) @@ TestAspect.before(truncateCollections) @@ TestAspect.sequential
  }.provide(
    testContextLayer,
    UserServiceLive.layer,
    UserRepositoryLive.layer,
    CollectionServiceLive.layer,
    CollectionRepositoryLive.layer,
    Repository.quillLayer,
    RecoveryTokensRepositoryLive.layer,
    ZLayer.succeed(RecoveryTokensConfig(24 * 3600)),
    JWTServiceLive.layer,
    dataSourceLayer,
    emailServiceLayer,
    Scope.default,
    ZLayer.succeed(JWTConfig("secret", 3600))
  )
}
