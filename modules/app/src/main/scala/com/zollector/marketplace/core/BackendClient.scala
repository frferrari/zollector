package com.zollector.marketplace.core

import zio.{Task, ZIO, ZLayer}
import sttp.client3.*
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets
import sttp.tapir.client.sttp.SttpClientInterpreter
import sttp.tapir.Endpoint
import sttp.client3.impl.zio.FetchZioBackend
import com.zollector.marketplace.http.endpoints.{CollectionEndpoints, UserEndpoints}
import com.zollector.marketplace.config.BackendClientConfig
import zio.URLayer
import zio.ULayer

case class RestrictedEndpointException(msg: String) extends RuntimeException(msg)

trait BackendClient {
  val collection: CollectionEndpoints
  val user: UserEndpoints
  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]
  def secureEndpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {
  override val collection: CollectionEndpoints = new CollectionEndpoints {}
  override val user: UserEndpoints             = new UserEndpoints {}

  private def endpointRequest[I, E, O](endpoint: Endpoint[Unit, I, E, O, Any]): I => Request[Either[E, O], Any] =
    interpreter
      .toRequestThrowDecodeFailures(endpoint, config.uri)

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] =
    interpreter
      .toSecureRequestThrowDecodeFailures(endpoint, config.uri)

  private def tokenOrFail =
    ZIO
      .fromOption(Session.getUserState)
      .orElseFail(RestrictedEndpointException("You need to log in."))
      .map(_.token)

  override def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] =
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve

  override def secureEndpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[String, I, E, O, Any])(
      payload: I
  ): Task[O] = {
    for {
      token    <- tokenOrFail
      response <- backend.send(secureEndpointRequest(endpoint)(token)(payload)).map(_.body).absolve
    } yield response
  }
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
