package com.zollector.marketplace.http.controllers

import com.zollector.marketplace.domain.commands.{CreateCollectionCommand, UpdateCollectionCommand}
import zio.*
import sttp.tapir.server.ServerEndpoint
import com.zollector.marketplace.domain.data.UserID
import com.zollector.marketplace.http.endpoints.CollectionEndpoints
import com.zollector.marketplace.services.{CollectionService, JWTService}

class CollectionController private (service: CollectionService, jwtService: JWTService)
    extends BaseController
    with CollectionEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        service
          .create(CreateCollectionCommand(userId.id, req.name, req.description, req.yearStart, req.yearEnd))
          .either
      }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => _ => service.getAll(userId.id).either }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => collectionId =>
        ZIO
          .attempt(collectionId.toLong)
          .flatMap(id => service.getById(id, userId.id))
          .either
      }

  val updateCollection: ServerEndpoint[Any, Task] =
    updateByIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => (collectionId, req) =>
        service
          .updateById(
            collectionId,
            userId.id,
            UpdateCollectionCommand(userId.id, req.name, req.description, req.yearStart, req.yearEnd)
          )
          .either
      }

  val deleteCollection: ServerEndpoint[Any, Task] =
    deleteByIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => collectionId =>
        service.deleteById(collectionId, userId.id).either
      }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getAll, getById, updateCollection, deleteCollection)
}

object CollectionController {
  val makeZIO = for {
    service    <- ZIO.service[CollectionService]
    jwtService <- ZIO.service[JWTService]
  } yield new CollectionController(service, jwtService)
}
