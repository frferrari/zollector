package com.zollector.marketplace.http.requests

import com.zollector.marketplace.domain.data.User
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

case class RegisterUserRequest(
    nickname: String,
    email: String,
    password: String,
    firstName: String,
    lastName: String
)

object RegisterUserRequest {
  given codec: JsonCodec[RegisterUserRequest] = DeriveJsonCodec.gen[RegisterUserRequest]
}
