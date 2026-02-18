package com.zollector.marketplace.config

import zio.Config
import zio.config.magnolia.*

final case class JWTConfig(secret: String, ttl: Long) derives Config
