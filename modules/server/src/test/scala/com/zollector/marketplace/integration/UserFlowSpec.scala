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
import com.zollector.marketplace.config.{JWTConfig, RecoveryTokensConfig}
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

    def deleteAuthorizedRequest[R: JsonCodec](
        path: String,
        payload: P,
        token: String
    ): Task[Option[R]] =
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

  val emailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] = ZLayer.succeed(new EmailServiceProbe)

  // Bob Lazar
  private val bobNewPassword = "bobNewPassword"
  private val registerUserRequestBob = RegisterUserRequest(
    nickname = "boblazar",
    email = "bob@zollector.com",
    password = "bobPassword",
    firstName = "bob",
    lastName = "lazar"
  )
  private val loginUserRequestBob =
    LoginRequest(registerUserRequestBob.email, registerUserRequestBob.password)
  private val updatePasswordRequestBob =
    UpdatePasswordRequest(registerUserRequestBob.email, registerUserRequestBob.password, bobNewPassword)
  private val deleteUserRequestBob =
    DeleteUserRequest(registerUserRequestBob.email, registerUserRequestBob.password)

  // Michio
  private val michioNewPassword = "michioNewPassword"
  private val registerUserRequestMichio = RegisterUserRequest(
    nickname = "michiokaku",
    email = "michio@zollector.com",
    password = "michioPassword",
    firstName = "michio",
    lastName = "kaku"
  )
  private val loginUserRequestMichio =
    LoginRequest(registerUserRequestMichio.email, registerUserRequestMichio.password)

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("Register a User") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequestBob)
        } yield assertTrue(maybeResponse.contains(UserResponse(registerUserRequestBob.email)))
      },
      test("Register a User and Log in this User") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequestBob)
          maybeToken    <- backendStub.postRequest[UserToken]("/users/login", loginUserRequestBob)
        } yield assertTrue(maybeToken.exists(_.email == registerUserRequestBob.email))
      },
      test("Register a User and Update its password") {
        for {
          backendStub   <- backendStubZIO
          maybeResponse <- backendStub.postRequest[UserResponse]("/users", registerUserRequestBob)
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", loginUserRequestBob)
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .putAuthorizedRequest[UserResponse](
              "/users/password",
              updatePasswordRequestBob,
              userToken.token
            )
          maybeOldToken <- backendStub.postRequest[UserToken]("/users/login", loginUserRequestBob)
          maybeNewToken <- backendStub.postRequest[UserToken](
            "/users/login",
            loginUserRequestBob.copy(password = updatePasswordRequestBob.newPassword)
          )
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("Register a User and Delete this User") {
        for {
          backendStub         <- backendStubZIO
          userRepo            <- ZIO.service[UserRepository]
          maybeResponse       <- backendStub.postRequest[UserResponse]("/users", registerUserRequestBob)
          maybeRegisteredUser <- userRepo.getByEmail(registerUserRequestBob.email)
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", loginUserRequestBob)
            .someOrFail(new RuntimeException("Authentication failed"))
          _ <- backendStub
            .deleteAuthorizedRequest[UserResponse](
              "/users",
              deleteUserRequestBob,
              userToken.token
            )
          maybeUser <- userRepo.getByEmail(registerUserRequestBob.email)
        } yield assertTrue(
          maybeRegisteredUser.nonEmpty &&
            maybeUser.isEmpty
        )
      },
      test("Recover Password Flow for 2 different Users") {
        for {
          backendStub       <- backendStubZIO
          userRepo          <- ZIO.service[UserRepository]
          emailServiceProbe <- ZIO.service[EmailServiceProbe]

          // Register a User
          _ <- backendStub.postRequest[UserResponse]("/users", registerUserRequestBob)
          _ <- backendStub.postRequest[UserResponse]("/users", registerUserRequestMichio)

          // Trigger Recover Password Flow
          _ <- backendStub.postRequestNoResponse(
            "/users/forgot",
            ForgotPasswordRequest(registerUserRequestBob.email)
          )
          _ <- backendStub.postRequestNoResponse(
            "/users/forgot",
            ForgotPasswordRequest(registerUserRequestMichio.email)
          )

          // Fetch the token I was sent by email
          bobToken <- emailServiceProbe
            .probeToken(registerUserRequestBob.email)
            .someOrFail(new RuntimeException("Token was not emailed"))
          michioToken <- emailServiceProbe
            .probeToken(registerUserRequestMichio.email)
            .someOrFail(new RuntimeException("Token was not emailed"))

          // Recover
          _ <- backendStub.postRequestNoResponse(
            "/users/recover",
            RecoverPasswordRequest(registerUserRequestBob.email, bobToken, bobNewPassword)
          )
          _ <- backendStub.postRequestNoResponse(
            "/users/recover",
            RecoverPasswordRequest(registerUserRequestMichio.email, michioToken, michioNewPassword)
          )

          maybeOldBobToken <- backendStub.postRequest[UserToken]("/users/login", loginUserRequestBob)
          maybeNewBobToken <- backendStub.postRequest[UserToken](
            "/users/login",
            loginUserRequestBob.copy(password = bobNewPassword)
          )

          maybeOldMichioToken <- backendStub.postRequest[UserToken]("/users/login", loginUserRequestMichio)
          maybeNewMichioToken <- backendStub.postRequest[UserToken](
            "/users/login",
            loginUserRequestMichio.copy(password = michioNewPassword)
          )

        } yield assertTrue(
          maybeOldBobToken.isEmpty && maybeNewBobToken.nonEmpty &&
            maybeOldMichioToken.isEmpty && maybeNewMichioToken.nonEmpty
        )
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.layer,
      emailServiceLayer,
      ZLayer.succeed(RecoveryTokensConfig(24 * 3600)),
      Repository.quillLayer,
      dataSourceLayer,
      Scope.default,
      ZLayer.succeed(JWTConfig("secret", 3600))
    )
}
