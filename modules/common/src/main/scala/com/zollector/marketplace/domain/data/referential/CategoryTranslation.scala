package com.zollector.marketplace.domain.data.referential

import com.zollector.marketplace.domain.data.ValueObjects.*

final case class CategoryTranslation(
    id: CategoryTranslationId,
    categoryId: CategoryId,
    language: LanguageCode,
    name: String,
    description: String,
    slug: Slug
)
