package com.zollector.marketplace.http.requests

import com.zollector.marketplace.domain.data.User
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

case class UpdatePasswordRequest(email: String, oldPassword: String, newPassword: String)
    derives JsonCodec
