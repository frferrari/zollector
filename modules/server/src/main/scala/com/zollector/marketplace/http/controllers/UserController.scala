package com.zollector.marketplace.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint
import com.zollector.marketplace.http.endpoints.*
import com.zollector.marketplace.http.responses.*
import com.zollector.marketplace.services.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.domain.errors.*

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController
    with UserEndpoints {

  val register: ServerEndpoint[Any, Task] = registerUserEndpoint
    .serverLogic { req =>
      userService
        .registerUser(req)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint
    .serverLogic { req =>
      userService
        .generateToken(req)
        .someOrFail(UnauthorizedException("Email or password is incorrect"))
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        userService
          .updatePassword(req)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserIdentifier, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        userService
          .deleteUser(req)
          .either
      }

  val forgotPassword: ServerEndpoint[Any, Task] =
    forgotPasswordEndpoint
      .serverLogic { req =>
        userService.sendPasswordRecoveryToken(req.email).either
      }

  val recoverPassword: ServerEndpoint[Any, Task] =
    recoverPasswordEndpoint
      .serverLogic { req =>
        userService
          .recoverPasswordFromToken(req.email, req.token, req.newPassword)
          .filterOrFail(b => b)(UnauthorizedException("The email/token combination is invalid"))
          .unit
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    register,
    updatePassword,
    delete,
    login,
    forgotPassword,
    recoverPassword
  )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield new UserController(userService, jwtService)
}
