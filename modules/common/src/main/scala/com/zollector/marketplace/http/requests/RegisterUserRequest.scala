package com.zollector.marketplace.http.requests

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

final case class RegisterUserRequest(
    nickname: String,
    email: Email,
    password: String,
    firstName: String,
    lastName: String
) derives JsonCodec
