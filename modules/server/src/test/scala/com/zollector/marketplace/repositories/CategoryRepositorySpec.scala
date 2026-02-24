package com.zollector.marketplace.repositories

import zio.*
import zio.test.*

import javax.sql.DataSource
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.data.referential.*
import com.zollector.marketplace.repositories.referential.*

object CategoryRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/categories.sql"

  private val postageStampsCategoryId = CategoryId(1L)

  private val (categoryStamps, categoryStampTranslations) = CreateCategoryCommand(
    isActive = true,
    translations = Map(
      LanguageCode.EN -> ("Stamps", "Stamps, sheetlets, ...", Slug("stamps")),
      LanguageCode.FR -> ("Timbres poste", "Timbres poste, blocs feuillet, blocs, ...", Slug("timbres-poste"))
    )
  ).toCategory

  private val (categoryPostcards, categoryPostcardTranslations) = CreateCategoryCommand(
    isActive = true,
    translations = Map(
      LanguageCode.EN -> ("Postcards", "Postcards and QSL cards", Slug("postcards")),
      LanguageCode.FR -> ("Cartes postales", "Cartes postales et cartes SQL", Slug("cartes-postales"))
    )
  ).toCategory

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CategoryRepositorySpec")(
      test("create a category") {
        for {
          repo            <- ZIO.service[CategoryRepository]
          categoryCreated <- repo.create(categoryStamps, categoryStampTranslations)
          _               <- repo.create(categoryPostcards, categoryPostcardTranslations)
          (fetchedCategory, fetchedCategoryTranslations) <- repo
            .getById(categoryCreated.id)
            .someOrFail("Could not fetch category after creation")
        } yield assertTrue(
          categoryCreated.isActive == categoryStamps.isActive &&
            fetchedCategory.isActive == categoryStamps.isActive &&
            fetchedCategoryTranslations
              .map(t => t.copy(id = CategoryTranslationId(-1L)))
              .toSet == categoryStampTranslations
              .map(t => t.copy(id = CategoryTranslationId(-1L), categoryId = categoryCreated.id))
              .toSet
        )
      },
      test("get a category by id") {
        for {
          repo            <- ZIO.service[CategoryRepository]
          categoryCreated <- repo.create(categoryStamps, categoryStampTranslations)
          _               <- repo.create(categoryPostcards, categoryPostcardTranslations)
          (fetchedCategory, fetchedCategoryTranslations) <- repo
            .getById(categoryCreated.id)
            .someOrFail("Could not fetch category after creation")
          notFoundCategory <- repo.getById(CategoryId(Long.MaxValue))
        } yield assertTrue(
          fetchedCategory.isActive == categoryStamps.isActive &&
            fetchedCategoryTranslations
              .map(t => t.copy(id = CategoryTranslationId(-1L)))
              .toSet == categoryStampTranslations
              .map(t => t.copy(id = CategoryTranslationId(-1L), categoryId = categoryCreated.id))
              .toSet &&
            notFoundCategory.isEmpty
        )
      },
      test("get localized Categories") {
        for {
          repo                     <- ZIO.service[CategoryRepository]
          categoryStampsCreated    <- repo.create(categoryStamps, categoryStampTranslations)
          categoryPostcardsCreated <- repo.create(categoryPostcards, categoryPostcardTranslations)
          enCategories             <- repo.getAllLocalized(LanguageCode.EN)
          frCategories             <- repo.getAllLocalized(LanguageCode.FR)
        } yield assertTrue {
          val expectedEN = (categoryStampTranslations
            .filter(_.language == LanguageCode.EN)
            .map(t => LocalizedCategory(categoryStampsCreated.id, t.name, t.description, t.slug))
            ++ categoryPostcardTranslations
              .filter(_.language == LanguageCode.EN)
              .map(t => LocalizedCategory(categoryPostcardsCreated.id, t.name, t.description, t.slug))).toSet

          val expectedFR = (categoryStampTranslations
            .filter(_.language == LanguageCode.FR)
            .map(t => LocalizedCategory(categoryStampsCreated.id, t.name, t.description, t.slug))
            ++ categoryPostcardTranslations
              .filter(_.language == LanguageCode.FR)
              .map(t => LocalizedCategory(categoryPostcardsCreated.id, t.name, t.description, t.slug))).toSet

          enCategories.toSet == expectedEN &&
          frCategories.toSet == expectedFR
        }
      }
    ).provide(CategoryRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
