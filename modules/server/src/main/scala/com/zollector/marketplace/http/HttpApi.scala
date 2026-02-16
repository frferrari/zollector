package com.zollector.marketplace.http

import com.zollector.marketplace.http.controllers.*
import sttp.tapir.server.ServerEndpoint
import zio.Task

object HttpApi {
  def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] =
    controllers.flatMap(_.routes)

  def makeControllers = for {
    healthController     <- HealthController.makeZIO
    collectionController <- CollectionController.makeZIO
  } yield List(healthController, collectionController)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
