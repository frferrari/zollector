package com.zollector.marketplace.http.controllers

import zio.*
import zio.test.*
import sttp.client3.*
import sttp.tapir.generic.auto.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.{DecoderOps, EncoderOps}

import java.time.Instant
import com.zollector.marketplace.domain.data.{Collection, User, UserID, UserToken}
import com.zollector.marketplace.http.requests.CreateCollectionRequest
import com.zollector.marketplace.services.{CollectionService, JWTService}
import com.zollector.marketplace.syntax.*

object CollectionControllerSpec extends ZIOSpecDefault {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val FranceCollection = Collection(
    1,
    "France 1960 to 1990",
    "Stamps from France",
    Some(1960),
    Some(1990),
    "france-1960-to-1990",
    Instant.now(),
    None
  )

  private val serviceStub = new CollectionService {
    override def create(req: CreateCollectionRequest): Task[Collection] =
      ZIO.succeed(FranceCollection)

    override def getById(id: Long): Task[Option[Collection]] = ZIO.succeed {
      if (id == 1) Some(FranceCollection)
      else None
    }

    override def getBySlug(slug: String): Task[Option[Collection]] = ZIO.succeed {
      if (slug == FranceCollection.slug) Some(FranceCollection)
      else None
    }

    override def getAll: Task[List[Collection]] = ZIO.succeed(List(FranceCollection))
  }

  private val jwtServiceStub = new JWTService {
    override def createToken(user: User): Task[UserToken] =
      ZIO.succeed(UserToken(user.email, "ALL_IS_GOOD", 99999999L))

    override def verityToken(token: String): Task[UserID] =
      ZIO.succeed(UserID(1, "bob@zollection.com"))
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
            .get(uri"/collections/1")
            .header("Authorization", "Bearer ALL_IS_GOOD")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption.flatMap(_.fromJson[Collection].toOption).contains(FranceCollection)
        }
      }
    )
      .provide(ZLayer.succeed(serviceStub), ZLayer.succeed(jwtServiceStub))
}
