package com.zollector.marketplace.http.requests

import com.zollector.marketplace.domain.data.ValueObjects.{CategoryId, FamilyId}
import zio.json.{DeriveJsonCodec, JsonCodec}

case class UpdateCollectionRequest(
    categoryId: CategoryId,
    familyId: FamilyId,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
)

object UpdateCollectionRequest {
  given codec: JsonCodec[UpdateCollectionRequest] = DeriveJsonCodec.gen[UpdateCollectionRequest]
}
