package com.zollector.marketplace.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}

case class CreateCollectionRequest(
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
)

object CreateCollectionRequest {
  given codec: JsonCodec[CreateCollectionRequest] = DeriveJsonCodec.gen[CreateCollectionRequest]
}
