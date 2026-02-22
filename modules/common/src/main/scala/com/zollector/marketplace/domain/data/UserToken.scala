package com.zollector.marketplace.domain.data

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.Email

case class UserToken(email: Email, token: String, expires: Long) derives JsonCodec
