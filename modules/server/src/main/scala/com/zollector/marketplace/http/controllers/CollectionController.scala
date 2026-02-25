package com.zollector.marketplace.http.controllers

import com.zollector.marketplace.domain.commands.{CreateCollectionCommand, UpdateCollectionCommand}
import zio.*
import sttp.tapir.server.ServerEndpoint
import com.zollector.marketplace.domain.data.UserIdentifier
import com.zollector.marketplace.http.endpoints.CollectionEndpoints
import com.zollector.marketplace.services.{CollectionService, JWTService}

class CollectionController private (service: CollectionService, jwtService: JWTService)
    extends BaseController
    with CollectionEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        service
          .create(
            CreateCollectionCommand(
              userId.id,
              req.categoryId,
              req.familyId,
              req.name,
              req.description,
              req.yearStart,
              req.yearEnd
            )
          )
          .either
      }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => _ => service.getAll(userId.id).either }

  val getAllTest: ServerEndpoint[Any, Task] =
    getAllTestEndpoint
      .serverLogicSuccess[Task](_ => service.getAllTest)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userIdentifier => collectionId =>
        service.getById(collectionId, userIdentifier.id).either
      }

  val updateCollection: ServerEndpoint[Any, Task] =
    updateByIdEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => (collectionId, req) =>
        service
          .updateById(
            collectionId,
            userId.id,
            UpdateCollectionCommand(
              userId.id,
              req.categoryId,
              req.familyId,
              req.name,
              req.description,
              req.yearStart,
              req.yearEnd
            )
          )
          .either
      }

  val deleteCollection: ServerEndpoint[Any, Task] =
    deleteByIdEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => collectionId =>
        service.deleteById(collectionId, userId.id).either
      }

  val allFacets: ServerEndpoint[Any, Task] =
    allFacetsEndpoint
      .serverLogic { _ => service.allFacets().either }

  val search: ServerEndpoint[Any, Task] =
    searchEndpoint.serverLogic { filter =>
      service.search(filter).either
    }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getAll, getAllTest, allFacets, search, getById, updateCollection, deleteCollection)
}

object CollectionController {
  val makeZIO = for {
    service    <- ZIO.service[CollectionService]
    jwtService <- ZIO.service[JWTService]
  } yield new CollectionController(service, jwtService)
}
