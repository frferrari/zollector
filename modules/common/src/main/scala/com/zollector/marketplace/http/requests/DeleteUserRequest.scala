package com.zollector.marketplace.http.requests

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

final case class DeleteUserRequest(email: Email, password: String) derives JsonCodec
