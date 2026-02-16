package com.zollector.marketplace.http.requests

import com.zollector.marketplace.domain.data.Collection
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

case class CreateCollectionRequest(
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
) {
  def toCollection(id: Long = -1L) =
    Collection(id, name, description, yearStart, yearEnd, Collection.makeSlug(name), Instant.now())
}

object CreateCollectionRequest {
  given codec: JsonCodec[CreateCollectionRequest] = DeriveJsonCodec.gen[CreateCollectionRequest]
}
