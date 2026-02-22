package com.zollector.marketplace.http.responses

import zio.json.JsonCodec

case class UserResponse(email: String) derives JsonCodec
