package com.zollector.marketplace.core

import zio.{Task, ZIO, ZLayer}
import sttp.client3.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.Endpoint
import sttp.client3.impl.zio.FetchZioBackend

import com.zollector.marketplace.http.endpoints.CollectionEndpoints
import com.zollector.marketplace.config.BackendClientConfig
import zio.URLayer
import zio.ULayer

trait BackendClient {
  val collection: CollectionEndpoints
  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {
  override val collection: CollectionEndpoints = new CollectionEndpoints {}

  private def endpointRequest[I, E, O](endpoint: Endpoint[Unit, I, E, O, Any]): I => Request[Either[E, O], Any] =
    interpreter
      .toRequestThrowDecodeFailures(endpoint, config.uri)

  override def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
}

object BackendClientLive {
  val layer: URLayer[SttpClientInterpreter & BackendClientConfig, BackendClient] = ZLayer {
    for {
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(FetchZioBackend(), interpreter, config)
  }

  val configuredLayer: ULayer[BackendClient] = ZLayer.succeed(
    new BackendClientLive(
      FetchZioBackend(),
      SttpClientInterpreter(),
      BackendClientConfig(Some(uri"http://localhost:8080"))
    )
  )
}
