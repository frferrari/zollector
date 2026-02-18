package com.zollector.marketplace.http.controllers

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
        service.create(req).either // TODO user the userId.id
      }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => _ => service.getAll.either } // TODO get all collections for that userId.id

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { _ => collectionId =>
        ZIO
          .attempt(collectionId.toLong)
          .flatMap(service.getById) // TODO use the userId.id
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CollectionController {
  val makeZIO = for {
    service    <- ZIO.service[CollectionService]
    jwtService <- ZIO.service[JWTService]
  } yield new CollectionController(service, jwtService)
}
