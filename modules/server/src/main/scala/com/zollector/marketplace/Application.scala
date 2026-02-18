package com.zollector.marketplace

import com.zollector.marketplace.config.{Configs, JWTConfig}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*
import com.zollector.marketplace.http.HttpApi
import com.zollector.marketplace.services.*
import com.zollector.marketplace.repositories.*

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(ZioHttpServerOptions.default).toHttp(endpoints)
    )
    _ <- Console.printLine("... Starting ...")
  } yield ()

  override def run = serverProgram.provide(
    Server.default,
    // Configs
    Configs.makeLayer[JWTConfig](Configs.CONFIG_JWT),
    // services
    CollectionServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.layer,
    // repos
    CollectionRepositoryLive.layer,
    UserRepositoryLive.layer,
    // other requirements
    Repository.dataLayer
  )
}
