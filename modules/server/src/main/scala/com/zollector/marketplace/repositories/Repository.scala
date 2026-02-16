package com.zollector.marketplace.repositories

import io.getquill.jdbczio.*
import io.getquill.SnakeCase

object Repository {
  def quillLayer =
    Quill.Postgres.fromNamingStrategy(SnakeCase)

  def dataSourceLayer =
    Quill.DataSource.fromPrefix("zollector.db")

  val dataLayer = dataSourceLayer >>> quillLayer
}
