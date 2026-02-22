package com.zollector.marketplace.http.requests

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

final case class RecoverPasswordRequest(email: Email, token: String, newPassword: String)
    derives JsonCodec
