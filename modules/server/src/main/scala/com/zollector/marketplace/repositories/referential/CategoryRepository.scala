package com.zollector.marketplace.repositories.referential

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.data.referential.{Category, CategoryTranslation, LocalizedCategory}
import com.zollector.marketplace.repositories.QuillMappings

trait CategoryRepository {
  def create(category: Category, translations: List[CategoryTranslation]): Task[Category]
  def getById(categoryId: CategoryId): Task[Option[(Category, List[CategoryTranslation])]]
  def getAllLocalized(language: LanguageCode): Task[List[LocalizedCategory]]
}

class CategoryRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends CategoryRepository with QuillMappings {

  import quill.*

  inline given categorySchema: SchemaMeta[Category]     = schemaMeta[Category]("referential.categories")
  inline given categoryInsertMeta: InsertMeta[Category] = insertMeta[Category](_.id, _.createdAt)

  inline given categoryTranslationSchema: SchemaMeta[CategoryTranslation] =
    schemaMeta[CategoryTranslation]("referential.category_translations")
  inline given categoryTranslationInsertMeta: InsertMeta[CategoryTranslation] = insertMeta[CategoryTranslation](_.id)

  override def create(category: Category, translations: List[CategoryTranslation]): Task[Category] =
    transaction {
      for {
        created <- run(query[Category].insertValue(lift(category)).returning(c => c))
        translationsWithId = translations.map(_.copy(categoryId = created.id))
        _ <- ZIO.foreach(translationsWithId)(t => run(query[CategoryTranslation].insertValue(lift(t))))
      } yield created
    }

  override def getById(categoryId: CategoryId): Task[Option[(Category, List[CategoryTranslation])]] =
    run(
      query[Category]
        .filter(_.id == lift(categoryId))
        .join(query[CategoryTranslation])
        .on((c, t) => c.id == t.categoryId)
    ).map { rows =>
      rows.headOption.map { case (category, _) => (category, rows.map(_._2)) }
    }

  override def getAllLocalized(languageCode: LanguageCode): Task[List[LocalizedCategory]] =
    run(
      query[CategoryTranslation]
        .filter(t => t.language == lift(languageCode) || t.language == lift(LanguageCode.EN))
    ).map(CategoryRepositoryLive.applyFallback(_, languageCode))
}

object CategoryRepositoryLive {
  private[referential] def applyFallback(
      rows: List[CategoryTranslation],
      lang: LanguageCode
  ): List[LocalizedCategory] =
    rows
      .groupBy(_.categoryId)
      .values
      .toList
      .flatMap { translations =>
        val preferred = translations.find(_.language == lang)
        val fallback  = translations.find(_.language == LanguageCode.EN)
        preferred.orElse(fallback).map(t => LocalizedCategory(t.categoryId, t.name, t.description, t.slug)).toList
      }

  val layer = ZLayer {
    ZIO
      .service[Quill.Postgres[SnakeCase.type]]
      .map(quill => CategoryRepositoryLive(quill))
  }
}
