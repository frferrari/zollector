package com.zollector.marketplace.http.requests

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

case class UpdatePasswordRequest(email: Email, oldPassword: String, newPassword: String)
    derives JsonCodec
