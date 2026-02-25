package com.zollector.marketplace.domain.queries

import com.zollector.marketplace.domain.data.ValueObjects.{CategoryId, FamilyId}
import zio.json.JsonCodec

// This reflects what a User as selected in the UI
final case class CollectionFilter(categories: List[CategoryId] = List(), families: List[FamilyId] = List())
    derives JsonCodec {
  def isEmpty: Boolean = categories.isEmpty && families.isEmpty
}

object CollectionFilter {
  val empty = CollectionFilter()
}
