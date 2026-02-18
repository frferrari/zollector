package com.zollector.marketplace.config

import zio.Config
import zio.config.magnolia.*

final case class EmailServiceConfig(host: String, port: Int, user: String, password: String) derives Config
