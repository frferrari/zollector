package com.zollector.marketplace.domain.commands

import java.time.Instant

import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.data.referential.*

final case class CreateCategoryCommand(isActive: Boolean, translations: Map[LanguageCode, (String, String, Slug)]) {
  def toCategory: (Category, List[CategoryTranslation]) = {
    val category = Category(CategoryId(-1L), isActive, Instant.now())
    val categoryTranslations =
      translations
        .map((languageCode, translation) =>
          CategoryTranslation(
            CategoryTranslationId(-1L),
            category.id,
            languageCode,
            translation._1,
            translation._2,
            translation._3
          )
        )
        .toList

    (category, categoryTranslations)
  }
}
