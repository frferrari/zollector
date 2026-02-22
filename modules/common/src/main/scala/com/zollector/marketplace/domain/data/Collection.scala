package com.zollector.marketplace.domain.data

import com.zollector.marketplace.domain.data.ValueObjects.*
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

final case class Collection(
    id: CollectionId,
    userId: UserId,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None,
    slug: Slug,
    image: Option[String] = None,
    createdAt: Instant,
    updatedAt: Option[Instant] = None
)

object Collection {
  given codec: JsonCodec[Collection] = DeriveJsonCodec.gen[Collection]

  def makeSlug(name: String): Slug =
    Slug(
      name
        .replaceAll(" +", " ")
        .split(" ")
        .map(_.toLowerCase())
        .mkString("-")
    )
}
