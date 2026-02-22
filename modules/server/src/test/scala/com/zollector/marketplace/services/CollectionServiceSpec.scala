package com.zollector.marketplace.services

import zio.*
import zio.test.*

import com.zollector.marketplace.domain.commands.CreateCollectionCommand
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.repositories.CollectionRepository
import com.zollector.marketplace.syntax.*
import com.zollector.marketplace.domain.data.ValueObjects.*

object CollectionServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CollectionService]

  val stubRepositoryLayer = ZLayer.succeed(
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

  private val bobUserId    = UserId.random
  private val michioUserId = UserId.random
  private val createCollectionCommand = CreateCollectionCommand(
    userId = bobUserId,
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

        program.assert { case (collection, userCollectionOpt, notFoundCollectionOpt) =>
          userCollectionOpt.map(_.id).contains(collection.id) &&
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

        program.assert { case (collection, userCollectionOpt, notFoundCollectionOpt) =>
          userCollectionOpt.map(_.id).contains(collection.id) &&
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
    ).provide(CollectionServiceLive.layer, stubRepositoryLayer)
}
