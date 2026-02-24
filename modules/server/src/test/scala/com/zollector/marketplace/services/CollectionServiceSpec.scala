package com.zollector.marketplace.services

import zio.*
import zio.test.*
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.repositories.referential.*
import com.zollector.marketplace.syntax.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.data.referential.{Category, CategoryTranslation, LocalizedCategory}

object CollectionServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CollectionService]

  val stubCollectionRepositoryLayer = ZLayer.succeed(
    new CollectionRepository {
      val db = collection.mutable.Map[CollectionId, Collection]()

      override def create(collection: Collection): Task[Collection] =
        ZIO.succeed {
          val nextId        = CollectionId.random
          val newCollection = collection.copy(id = nextId)
          db += (nextId -> newCollection)
          newCollection
        }

      override def getById(id: CollectionId, userId: UserId): Task[Option[Collection]] =
        ZIO.succeed(db.values.find(c => c.id == id && c.userId == userId))

      override def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]] =
        ZIO.succeed(db.values.find(c => c.slug == slug && c.userId == userId))

      override def getAll(userId: UserId): Task[List[Collection]] =
        ZIO.succeed(db.values.filter(_.userId == userId).toList)

      override def getAllTest: Task[List[Collection]] =
        ZIO.succeed(db.values.toList)

      override def updateById(id: CollectionId, userId: UserId, collection: Collection): Task[Option[Collection]] =
        ZIO.attempt {
          db.get(id) match {
            case Some(_) => {
              db += (id -> collection)
              Some(collection)
            }
            case None => None
          }
        }

      override def updateBySlug(slug: Slug, userId: UserId, collection: Collection): Task[Option[Collection]] =
        ZIO.attempt {
          db.values.find(_.slug == slug) match {
            case Some(c) =>
              db += (c.id -> collection)
              Some(collection)
            case None => None
          }
        }

      override def deleteById(id: CollectionId, userId: UserId): Task[Boolean] =
        ZIO.attempt {
          db -= id
          true
        }

      override def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean] =
        ZIO.attempt {
          db.values.find(_.slug == slug) match {
            case Some(c) =>
              db -= c.id
              true

            case None =>
              false
          }
        }
    }
  )

  val stubCategoryRepositoryLayer = ZLayer.succeed(
    new CategoryRepository {
      def create(category: Category, translations: List[CategoryTranslation]): Task[Category] =
        ZIO.fail(new RuntimeException("Not implemented"))

      def getById(categoryId: CategoryId): Task[Option[(Category, List[CategoryTranslation])]] =
        ZIO.fail(new RuntimeException("Not implemented"))

      def getAllLocalized(language: LanguageCode): Task[List[LocalizedCategory]] =
        ZIO.succeed(
          List(
            LocalizedCategory(CategoryId(1L), "Stamps", "Postage stamps, sheetlets, blocs", Slug("stamps")),
            LocalizedCategory(CategoryId(2L), "Postcards", "Postcards and QSL cards", Slug(""))
          )
        )
    }
  )

  private val bobUserId    = UserId.random
  private val michioUserId = UserId.random

  private val postageStampsCategoryId = CategoryId(1L)
  private val singleStampsFamilyId    = FamilyId(1L)

  private val createCollectionCommand = CreateCollectionCommand(
    userId = bobUserId,
    categoryId = postageStampsCategoryId,
    familyId = singleStampsFamilyId,
    name = "Norway 1960 1990",
    description = "Stamps of Norway from 1960 to 1990",
    yearStart = Some(1960),
    yearEnd = Some(1990)
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CollectionServiceSpec")(
      test("create a collection") {
        val collectionZIO = service(_.create(createCollectionCommand))

        collectionZIO.assert { collection =>
          collection.categoryId == createCollectionCommand.categoryId &&
          collection.familyId == createCollectionCommand.familyId &&
          collection.name == createCollectionCommand.name &&
          collection.description == createCollectionCommand.description &&
          collection.yearStart == createCollectionCommand.yearStart &&
          collection.yearEnd == createCollectionCommand.yearEnd
        }
      },
      test("getById returns the collection matching the slug and the user") {
        val program = for {
          collection            <- service(_.create(createCollectionCommand))
          userCollectionOpt     <- service(_.getById(collection.id, createCollectionCommand.userId))
          notFoundCollectionOpt <- service(_.getBySlug(collection.slug, UserId.random))
        } yield (collection, userCollectionOpt, notFoundCollectionOpt)

        program.assert {
          case (collection, userCollectionOpt: Option[Collection], notFoundCollectionOpt: Option[Collection]) =>
            userCollectionOpt.map(_.id).contains(collection.id) &&
            userCollectionOpt.map(_.categoryId).contains(createCollectionCommand.categoryId) &&
            userCollectionOpt.map(_.familyId).contains(createCollectionCommand.familyId) &&
            userCollectionOpt.map(_.name).contains(createCollectionCommand.name) &&
            userCollectionOpt.map(_.description).contains(createCollectionCommand.description) &&
            userCollectionOpt.map(_.yearStart).contains(createCollectionCommand.yearStart) &&
            userCollectionOpt.map(_.yearEnd).contains(createCollectionCommand.yearEnd) &&
            userCollectionOpt.map(_.slug).contains(collection.slug) &&
            notFoundCollectionOpt.isEmpty
        }
      },
      test("getBySlug returns the collection matching the slug and the user") {
        val program = for {
          collection            <- service(_.create(createCollectionCommand))
          userCollectionOpt     <- service(_.getBySlug(collection.slug, createCollectionCommand.userId))
          notFoundCollectionOpt <- service(_.getBySlug(collection.slug, UserId.random))
        } yield (collection, userCollectionOpt, notFoundCollectionOpt)

        program.assert { case (collection, userCollectionOpt: Option[Collection], notFoundCollectionOpt) =>
          userCollectionOpt.map(_.id).contains(collection.id) &&
          userCollectionOpt.map(_.categoryId).contains(createCollectionCommand.categoryId) &&
          userCollectionOpt.map(_.familyId).contains(createCollectionCommand.familyId) &&
          userCollectionOpt.map(_.name).contains(createCollectionCommand.name) &&
          userCollectionOpt.map(_.description).contains(createCollectionCommand.description) &&
          userCollectionOpt.map(_.yearStart).contains(createCollectionCommand.yearStart) &&
          userCollectionOpt.map(_.yearEnd).contains(createCollectionCommand.yearEnd) &&
          userCollectionOpt.map(_.slug).contains(collection.slug) &&
          notFoundCollectionOpt.isEmpty
        }
      },
      test("getAll collections returns collections belonging to the proper user") {
        val program = for {
          bobCollection1 <- service(
            _.create(
              CreateCollectionCommand(
                userId = bobUserId,
                categoryId = postageStampsCategoryId,
                familyId = singleStampsFamilyId,
                name = "Norway 1960 1990",
                description = "Stamps of Norway from 1960 to 1990",
                yearStart = Some(1960),
                yearEnd = Some(1990)
              )
            )
          )
          bobCollection2 <- service(
            _.create(
              CreateCollectionCommand(
                userId = bobUserId,
                categoryId = postageStampsCategoryId,
                familyId = singleStampsFamilyId,
                name = "Finland 1950 2000",
                description = "Stamps of Finland from 1950 to 2000",
                yearStart = Some(1950),
                yearEnd = Some(2000)
              )
            )
          )
          michioCollection1 <- service(
            _.create(
              CreateCollectionCommand(
                userId = michioUserId,
                categoryId = postageStampsCategoryId,
                familyId = singleStampsFamilyId,
                name = "Sweden 1950 2000",
                description = "Stamps of Sweden from 1950 to 2000",
                yearStart = Some(1950),
                yearEnd = Some(2000)
              )
            )
          )
          bobCollections    <- service(_.getAll(bobUserId))
          michioCollections <- service(_.getAll(michioUserId))
        } yield (bobCollections, michioCollections, bobCollection1, bobCollection2, michioCollection1)

        program.assert { case (bobCollections, michioCollections, bobCollection1, bobCollection2, michioCollection1) =>
          bobCollections.toSet == Set(bobCollection1, bobCollection2) &&
          michioCollections.toSet == Set(michioCollection1)
        }
      }
    ).provide(
      CollectionServiceLive.layer,
      stubCollectionRepositoryLayer,
      stubCategoryRepositoryLayer
    )
}
