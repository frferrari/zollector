package com.zollector.marketplace.http.requests

import zio.json.JsonCodec

final case class RegisterUserRequest(
    nickname: String,
    email: String,
    password: String,
    firstName: String,
    lastName: String
) derives JsonCodec
