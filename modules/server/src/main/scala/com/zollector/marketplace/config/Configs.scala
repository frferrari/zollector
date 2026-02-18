package com.zollector.marketplace.config

import zio.*
import zio.Config
import zio.config.typesafe.TypesafeConfigProvider

object Configs {

  val CONFIG_JWT = "zollector.jwt"
  val CONFIG_RECOVERY_TOKENS = "zollector.recoverytokens"
  val CONFIG_EMAIL = "zollector.email"

  def makeLayer[C: {Config, Tag}](path: String): ZLayer[Any, Config.Error, C] =
    ZLayer.fromZIO {
      val provider = path.split("\\.").foldRight(TypesafeConfigProvider.fromResourcePath()) {
        (segment, provider) => provider.nested(segment)
      }
      ZIO.config[C].withConfigProvider(provider)
    }
}
