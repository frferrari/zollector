package com.zollector.marketplace.config

import zio.Config
import zio.config.magnolia.*

final case class RecoveryTokensConfig(duration: Long) derives Config
