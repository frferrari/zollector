package com.zollector.marketplace.http.responses

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

case class UserResponse(email: Email) derives JsonCodec
