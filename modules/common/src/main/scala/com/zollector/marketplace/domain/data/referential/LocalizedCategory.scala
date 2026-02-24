package com.zollector.marketplace.domain.data.referential

import com.zollector.marketplace.domain.data.ValueObjects.*
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class LocalizedCategory(id: CategoryId, name: String, description: String, slug: Slug)
object LocalizedCategory:
  given codec: JsonCodec[LocalizedCategory] = DeriveJsonCodec.gen[LocalizedCategory]
