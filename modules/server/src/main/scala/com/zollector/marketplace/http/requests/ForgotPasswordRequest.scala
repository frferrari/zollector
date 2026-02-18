package com.zollector.marketplace.http.requests

import zio.json.JsonCodec

final case class ForgotPasswordRequest(email: String) derives JsonCodec
