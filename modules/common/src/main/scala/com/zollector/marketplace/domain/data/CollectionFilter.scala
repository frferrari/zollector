package com.zollector.marketplace.domain.data

import zio.json.JsonCodec
import com.zollector.marketplace.domain.data.ValueObjects.{CategoryId, FamilyId}
import com.zollector.marketplace.domain.data.referential.LocalizedCategory

final case class CollectionFilter(categories: List[LocalizedCategory] = List(), families: List[FamilyId] = List())
    derives JsonCodec

object CollectionFilter {
  val empty = CollectionFilter()
}
