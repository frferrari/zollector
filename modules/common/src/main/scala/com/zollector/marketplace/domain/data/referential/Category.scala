package com.zollector.marketplace.domain.data.referential

import com.zollector.marketplace.domain.data.ValueObjects.*
import java.time.Instant

final case class Category(id: CategoryId, isActive: Boolean, createdAt: Instant)
