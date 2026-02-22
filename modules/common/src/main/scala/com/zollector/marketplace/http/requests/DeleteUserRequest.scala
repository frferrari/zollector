package com.zollector.marketplace.http.requests

import zio.json.JsonCodec

final case class DeleteUserRequest(email: String, password: String) derives JsonCodec
