package com.zollector.marketplace.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}
import java.time.Instant

final case class Collection(
    id: Long,
    userId: Long,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None,
    slug: String,
    createdAt: Instant,
    updatedAt: Option[Instant] = None
)

object Collection {
  given codec: JsonCodec[Collection] = DeriveJsonCodec.gen[Collection]

  def makeSlug(name: String): String =
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-")
}
