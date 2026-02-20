package com.zollector.marketplace.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}

case class UpdateCollectionRequest(
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
)

object UpdateCollectionRequest {
  given codec: JsonCodec[UpdateCollectionRequest] = DeriveJsonCodec.gen[UpdateCollectionRequest]
}
