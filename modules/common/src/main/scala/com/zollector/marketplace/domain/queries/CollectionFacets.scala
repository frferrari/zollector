package com.zollector.marketplace.domain.queries

import com.zollector.marketplace.domain.data.referential.LocalizedCategory
import zio.json.JsonCodec

// This contains the list of all possible filters used for example in the Collection filter panel
final case class CollectionFacets(categories: List[LocalizedCategory] = List.empty) derives JsonCodec

object CollectionFacets {
  val empty = CollectionFacets()
}
