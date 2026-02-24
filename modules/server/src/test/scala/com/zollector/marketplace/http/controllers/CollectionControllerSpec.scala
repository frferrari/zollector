package com.zollector.marketplace.http.controllers

import zio.*
import zio.test.*
import zio.json.*
import sttp.client3.*
import sttp.tapir.generic.auto.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError

import java.time.Instant
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.services.*
import com.zollector.marketplace.syntax.*

object CollectionControllerSpec extends ZIOSpecDefault {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val postageStampsCategoryId = CategoryId(1L)
  private val singleStampsFamilyId    = FamilyId(1L)

  private val FranceCollection = Collection(
    CollectionId.random,
    UserId.random,
    postageStampsCategoryId,
    singleStampsFamilyId,
    "France 1960 to 1990",
    "Stamps from France",
    Some(1960),
    Some(1990),
    Slug("france-1960-to-1990"),
    None,
    Instant.now(),
    None
  )

  private val serviceStub = new CollectionService {
    override def create(cmd: CreateCollectionCommand): Task[Collection] =
      ZIO.succeed(FranceCollection)

    override def getById(id: CollectionId, userId: UserId): Task[Option[Collection]] = ZIO.succeed {
      if (id == FranceCollection.id && userId == FranceCollection.userId) Some(FranceCollection)
      else None
    }

    override def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]] = ZIO.succeed {
      if (slug == FranceCollection.slug && userId == FranceCollection.userId) Some(FranceCollection)
      else None
    }

    override def getAll(userId: UserId): Task[List[Collection]] =
      ZIO.succeed(List(FranceCollection))

    override def getAllTest: Task[List[Collection]] =
      ZIO.succeed(List(FranceCollection))

    override def updateById(id: CollectionId, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
      ZIO.succeed {
        if (id == FranceCollection.id && userId == FranceCollection.userId) Some(cmd.toCollection())
        else None
      }

    override def updateBySlug(slug: Slug, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
      ZIO.succeed(None)

    override def deleteById(id: CollectionId, userId: UserId): Task[Boolean] = ZIO.succeed {
      if (id == FranceCollection.id && userId == FranceCollection.userId) true
      else false
    }

    override def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean] = ZIO.succeed(false)

    override def allFilters(languageCode: LanguageCode = LanguageCode.EN): Task[CollectionFilter] =
      ZIO.fail(new RuntimeException("Not implemented yet"))
  }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.email, "ALL_IS_GOOD", 99999999L))

    override def verityToken(token: String): Task[UserIdentifier] =
      ZIO.succeed(UserIdentifier(FranceCollection.userId, Email("bob@zollection.com")))
  }

  private def backendStubZIO(endpoint: CollectionController => ServerEndpoint[Any, Task]) = for {
    controller <- CollectionController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpoint(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CollectionControllerSpec")(
      test("create a Collection") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/collections")
            .body(
              CreateCollectionRequest(
                FranceCollection.categoryId,
                FranceCollection.familyId,
                FranceCollection.name,
                FranceCollection.description
              ).toJson
            )
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          val collection = respBody.toOption.flatMap(_.fromJson[Collection].toOption)

          collection.contains(
            FranceCollection.copy(createdAt = collection.map(_.createdAt).getOrElse(Instant.now()))
          )
        }
      },
      test("getAll Collections") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/collections")
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[List[Collection]].toOption)
            .contains(List(FranceCollection))
        }
      },
      test("getById") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/collections/${FranceCollection.id}")
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption.flatMap(_.fromJson[Collection].toOption).contains(FranceCollection)
        }
      },
      test("update a Collection") {
        val program = for {
          backendStub <- backendStubZIO(_.updateCollection)
          updateResponse <- basicRequest
            .put(uri"/collections/${FranceCollection.id}")
            .body(
              UpdateCollectionRequest(
                FranceCollection.categoryId,
                FranceCollection.familyId,
                FranceCollection.name,
                FranceCollection.description
              ).toJson
            )
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
            .map(_.body.toOption.flatMap(_.fromJson[Collection].toOption))
        } yield updateResponse

        program.assert { case updateResponse =>
          updateResponse.nonEmpty
        }
      },
      test("delete a Collection") {
        val program = for {
          backendStub <- backendStubZIO(_.deleteCollection)
          deleteResponse <- basicRequest
            .delete(uri"/collections/${FranceCollection.id}")
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
            .map(_.body.toOption.flatMap(_.fromJson[Boolean].toOption))
        } yield deleteResponse

        program.assert { case deleteResponse =>
          deleteResponse.contains(true)
        }
      }
    )
      .provide(ZLayer.succeed(serviceStub), ZLayer.succeed(jwtServiceStub))
}
