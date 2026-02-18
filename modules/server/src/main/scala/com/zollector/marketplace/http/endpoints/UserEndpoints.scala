package com.zollector.marketplace.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.http.responses.*
import com.zollector.marketplace.domain.data.*

trait UserEndpoints extends BaseEndpoint {
  val registerUserEndpoint =
    baseEndpoint
      .tag("Users")
      .name("register")
      .description("Register a User account")
      .in("users")
      .post
      .in(jsonBody[RegisterUserRequest])
      .out(jsonBody[UserResponse])

  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("update password")
      .description("Update user password")
      .in("users" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  val deleteEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("delete user account")
      .description("Delete a User account")
      .in("users")
      .delete
      .in(jsonBody[DeleteUserRequest])
      .out(jsonBody[Boolean])

  val loginEndpoint =
    baseEndpoint
      .tag("Users")
      .name("log in")
      .description("Log in and generate a JWT token")
      .in("users" / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken]) // TODO Use a specific response ?

  val forgotPasswordEndpoint =
    baseEndpoint
      .tag("Users")
      .name("forgot password")
      .description("Send email for password recovery")
      .in("users" / "forgot")
      .post
      .in(jsonBody[ForgotPasswordRequest])

  val recoverPasswordEndpoint =
    baseEndpoint
      .tag("Users")
      .name("recover password")
      .description("Set new password based on OTP")
      .in("users" / "recover")
      .post
      .in(jsonBody[RecoverPasswordRequest])
}
