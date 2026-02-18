package com.zollector.marketplace.http.controllers

import zio.*
import sttp.tapir.server.ServerEndpoint
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.domain.errors.*
import com.zollector.marketplace.http.endpoints.UserEndpoints
import com.zollector.marketplace.http.responses.UserResponse
import com.zollector.marketplace.services.*

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
        .someOrFail(UnauthorizedException)
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        userService
          .updatePassword(req)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserID, Task](token => jwtService.verityToken(token).either)
      .serverLogic { userId => req =>
        userService
          .deleteUser(req)
          .either
      }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    register,
    updatePassword,
    delete,
    login
  )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield new UserController(userService, jwtService)
}
