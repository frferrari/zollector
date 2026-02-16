package com.zollector.marketplace.http.controllers

import sttp.tapir.server.ServerEndpoint
import zio.*
import com.zollector.marketplace.http.endpoints.CollectionEndpoints
import com.zollector.marketplace.services.CollectionService

class CollectionController private (service: CollectionService)
    extends BaseController
    with CollectionEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic { req =>
    service.create(req).either
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic { _ => service.getAll.either }

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(service.getById)
      .either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CollectionController {
  val makeZIO = for {
    service <- ZIO.service[CollectionService]
  } yield new CollectionController(service)
}
