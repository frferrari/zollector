package com.zollector.marketplace.integration

import zio.*
import zio.test.*
import zio.json.*
import sttp.client3.*
import sttp.tapir.generic.auto.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import com.zollector.marketplace.config.JWTConfig
import com.zollector.marketplace.http.controllers.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.http.responses.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.services.*
import com.zollector.marketplace.domain.data.UserToken
import sttp.model.Method

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/integration.sql"

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val backendStubZIO =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

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

    def deleteAuthorizedRequest[R: JsonCodec](
        path: String,
        payload: P,
        token: String
    ): Task[Option[R]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  private val registerUserRequest = RegisterUserRequest(
    nickname = "boblazar",
    email = "admin@zollector.com",
    password = "bobPassword",
    firstName = "bob",
    lastName = "lazar"
  )

  private val loginUserRequest =
    LoginRequest(registerUserRequest.email, registerUserRequest.password)

  private val updatePasswordRequest =
    UpdatePasswordRequest(registerUserRequest.email, registerUserRequest.password, "newPassword")

  private val deleteUserRequest =
    DeleteUserRequest(registerUserRequest.email, registerUserRequest.password)

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("Register a User") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequest)
        } yield assertTrue(maybeResponse.contains(UserResponse(registerUserRequest.email)))
      },
      test("Register a User and Log in this User") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequest)
          maybeToken    <- backendStub.postRequest[UserToken]("/users/login", loginUserRequest)
        } yield assertTrue(maybeToken.exists(_.email == registerUserRequest.email))
      },
      test("Register a User and Update its password") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequest)
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", loginUserRequest)
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .putAuthorizedRequest[UserResponse](
              "/users/password",
              updatePasswordRequest,
              userToken.token
            )
          maybeOldToken <- backendStub.postRequest[UserToken]("/users/login", loginUserRequest)
          maybeNewToken <- backendStub.postRequest[UserToken](
            "/users/login",
            loginUserRequest.copy(password = updatePasswordRequest.newPassword)
          )
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("Register a User and Delete this User") {
        for {
          backendStub   <- backendStubZIO
          userRepo      <- ZIO.service[UserRepository]
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequest)
          maybeRegisteredUser <- userRepo.getByEmail(registerUserRequest.email)
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", loginUserRequest)
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .deleteAuthorizedRequest[UserResponse](
              "/users",
              deleteUserRequest,
              userToken.token
            )
          maybeUser <- userRepo.getByEmail(registerUserRequest.email)
        } yield assertTrue(
          maybeRegisteredUser.nonEmpty &&
            maybeUser.isEmpty
        )
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      Scope.default,
      ZLayer.succeed(JWTConfig("secret", 3600))
    )
}
