package com.zollector.marketplace.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.{ZIO, ZLayer}

import javax.sql.DataSource

trait RepositorySpec {

  val initScript: String

  private def createContainer = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)

    container.start()
    container
  }

  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setUrl(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)

    dataSource
  }

  val dataSourceLayer = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createContainer))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }

}
