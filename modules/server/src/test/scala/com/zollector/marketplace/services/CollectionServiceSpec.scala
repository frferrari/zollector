package com.zollector.marketplace.services

import zio.*
import zio.test.*
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.http.requests.CreateCollectionRequest
import com.zollector.marketplace.repositories.CollectionRepository
import com.zollector.marketplace.syntax.*

object CollectionServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CollectionService]

  val stubRepositoryLayer = ZLayer.succeed(
    new CollectionRepository {
      val db = collection.mutable.Map[Long, Collection]()

      override def create(collection: Collection): Task[Collection] =
        ZIO.succeed {
          val nextId        = db.keys.maxOption.getOrElse(0L) + 1L
          val newCollection = collection.copy(id = nextId)
          db += (nextId -> newCollection)
          newCollection
        }

      override def getById(id: Long): Task[Option[Collection]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Collection]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def getAll: Task[List[Collection]] =
        ZIO.succeed(db.values.toList)

      override def update(id: Long, collection: Collection): Task[Collection] =
        ZIO.attempt {
          val collection = db(id)
          db += (id -> collection)
          collection
        }

      override def delete(id: Long): Task[Boolean] =
        ZIO.attempt {
          db -= id
          true
        }
    }
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CollectionServiceSpec")(
      test("create a collection") {
        val collectionZIO = service(
          _.create(
            CreateCollectionRequest(
              name = "Norway 1960 1990",
              description = "Stamps of Norway from 1960 to 1990",
              yearStart = Some(1960),
              yearEnd = Some(1990)
            )
          )
        )

        collectionZIO.assert { collection =>
          collection.name == "Norway 1960 1990" &&
          collection.description == "Stamps of Norway from 1960 to 1990" &&
          collection.yearStart.contains(1960) &&
          collection.yearEnd.contains(1990)
        }
      },
      test("getById") {
        val program = for {
          collection <- service(
            _.create(
              CreateCollectionRequest(
                name = "Norway 1960 1990",
                description = "Stamps of Norway from 1960 to 1990",
                yearStart = Some(1960),
                yearEnd = Some(1990)
              )
            )
          )
          collectionOpt <- service(_.getById(collection.id))
        } yield (collection, collectionOpt)

        program.assert { case (collection, collectionOpt) =>
          collectionOpt.map(_.id).contains(collection.id) &&
          collection.name == "Norway 1960 1990" &&
          collection.description == "Stamps of Norway from 1960 to 1990" &&
          collection.yearStart.contains(1960) &&
          collection.yearEnd.contains(1990) &&
          collectionOpt.map(_.slug).contains(collection.slug)
        }
      },
      test("getBySlug") {
        val program = for {
          collection <- service(
            _.create(
              CreateCollectionRequest(
                name = "Norway 1960 1990",
                description = "Stamps of Norway from 1960 to 1990",
                yearStart = Some(1960),
                yearEnd = Some(1990)
              )
            )
          )
          collectionOpt <- service(_.getBySlug(collection.slug))
        } yield (collection, collectionOpt)

        program.assert { case (collection, collectionOpt) =>
          collectionOpt.map(_.id).contains(collection.id) &&
          collection.name == "Norway 1960 1990" &&
          collection.description == "Stamps of Norway from 1960 to 1990" &&
          collection.yearStart.contains(1960) &&
          collection.yearEnd.contains(1990) &&
          collectionOpt.map(_.slug).contains(collection.slug)
        }
      },
      test("getAll") {
        val program = for {
          collection1 <- service(
            _.create(
              CreateCollectionRequest(
                name = "Norway 1960 1990",
                description = "Stamps of Norway from 1960 to 1990",
                yearStart = Some(1960),
                yearEnd = Some(1990)
              )
            )
          )
          collection2 <- service(
            _.create(
              CreateCollectionRequest(
                name = "Sweden 1950 2000",
                description = "Stamps of Sweden from 1950 to 2000",
                yearStart = Some(1950),
                yearEnd = Some(2000)
              )
            )
          )
          collections <- service(_.getAll)
        } yield (collections, collection1, collection2)

        program.assert { case (collections, collection1, collection2) =>
          collections.toSet == Set(collection1, collection2)
        }
      }
    ).provide(CollectionServiceLive.layer, stubRepositoryLayer)
}
