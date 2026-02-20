package com.zollector.marketplace.integration

import zio.*
import zio.json.*
import sttp.client3.*
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import com.zollector.marketplace.domain.data.{Collection, UserToken}
import com.zollector.marketplace.http.requests.{
  CreateCollectionRequest,
  LoginRequest,
  RegisterUserRequest,
  UpdateCollectionRequest
}
import com.zollector.marketplace.http.responses.UserResponse
import com.zollector.marketplace.services.EmailService

trait IntegrationSpec {

  given zioME: MonadError[Task] = new RIOMonadError[Any]

  extension (backend: SttpBackend[Task, Nothing]) {
    def sendRequestNoPayload[R: JsonCodec](
        method: Method,
        path: String,
        maybeToken: Option[String] = None
    ): Task[Option[R]] =
      basicRequest
        .method(method, uri"$path")
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(json => json.fromJson[R].toOption))

    def getAuthorizedRequestNoPayload[R: JsonCodec](path: String, token: String): Task[Option[R]] =
      sendRequestNoPayload(Method.GET, path, Some(token))

    def deleteAuthorizedRequestNoPayload[R: JsonCodec](path: String, token: String): Task[Option[R]] =
      sendRequestNoPayload(Method.DELETE, path, Some(token))
  }

  extension [P: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[R: JsonCodec](
        method: Method,
        path: String,
        payload: P,
        maybeToken: Option[String] = None
    ): Task[Option[R]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(json => json.fromJson[R].toOption))

    def postRequest[R: JsonCodec](path: String, payload: P): Task[Option[R]] =
      sendRequest(Method.POST, path, payload, None)

    def postRequestNoResponse(path: String, payload: P): Task[Unit] =
      basicRequest
        .method(Method.POST, uri"$path")
        .body(payload.toJson)
        .send(backend)
        .unit

    def postAuthorizedRequest[R: JsonCodec](
        path: String,
        payload: P,
        token: String
    ): Task[Option[R]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def putRequest[R: JsonCodec](path: String, payload: P): Task[Option[R]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuthorizedRequest[R: JsonCodec](
        path: String,
        payload: P,
        token: String
    ): Task[Option[R]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def deleteRequest[R: JsonCodec](path: String, payload: P): Task[Option[R]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuthorizedRequest[R: JsonCodec](path: String, payload: P, token: String): Task[Option[R]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  class EmailServiceProbe extends EmailService {
    val db = collection.mutable.Map[String, String]()

    override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db += (to -> token))

    def probeToken(email: String): Task[Option[String]] =
      ZIO.succeed(db.get(email))
  }

  def registerUser(
      registerRequest: RegisterUserRequest,
      backendStub: SttpBackend[Task, Nothing]
  ): Task[UserResponse] =
    backendStub
      .postRequest[UserResponse]("/users", registerRequest)
      .someOrFail(new RuntimeException("User Registration failed"))

  def loginUser(loginRequest: LoginRequest, backendStub: SttpBackend[Task, Nothing]): Task[UserToken] =
    backendStub
      .postRequest[UserToken]("/users/login", loginRequest)
      .someOrFail(new RuntimeException("Authentication failed"))

  def createCollection(
      createCollectionRequest: CreateCollectionRequest,
      userToken: UserToken,
      backendStub: SttpBackend[Task, Nothing]
  ): Task[Option[Collection]] =
    backendStub
      .postAuthorizedRequest[Collection]("/collections", createCollectionRequest, userToken.token)

  def getCollection(
      collectionId: Long,
      userToken: UserToken,
      backendStub: SttpBackend[Task, Nothing]
  ): Task[Option[Collection]] =
    backendStub
      .getAuthorizedRequestNoPayload[Collection](s"/collections/$collectionId", userToken.token)

  def updateCollection(
      collectionId: Long,
      updateCollectionRequest: UpdateCollectionRequest,
      userToken: UserToken,
      backendStub: SttpBackend[Task, Nothing]
  ): Task[Option[Collection]] =
    backendStub
      .putAuthorizedRequest[Collection](s"/collections/$collectionId", updateCollectionRequest, userToken.token)

  def deleteCollection(
      collectionId: Long,
      userToken: UserToken,
      backendStub: SttpBackend[Task, Nothing]
  ): Task[Option[Boolean]] =
    backendStub
      .deleteAuthorizedRequestNoPayload[Boolean](s"/collections/$collectionId", userToken.token)
}
