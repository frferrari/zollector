package com.zollector.marketplace.repositories

import zio.*
import zio.test.*
import javax.sql.DataSource
import com.zollector.marketplace.http.requests.CreateCollectionRequest
import com.zollector.marketplace.repositories.*

object CollectionRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  private val collection1 = CreateCollectionRequest(
    name = "Norway 1960 1990",
    description = "Stamps of Norway from 1960 to 1990",
    yearStart = Some(1960),
    yearEnd = Some(1990)
  ).toCollection()

  private val collection2 = CreateCollectionRequest(
    name = "Sweden 1940 1970",
    description = "Stamps of Sweden from 1940 to 1970",
    yearStart = Some(1940),
    yearEnd = Some(1970)
  ).toCollection()

  private val updatedCollection1 = collection1.copy(
    name = "Norway 1950 2000",
    description = "Stamps of Norway from 1950 to 2000",
    yearStart = Some(1950),
    yearEnd = Some(2000)
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CollectionRepositorySpec")(
      test("create a collection") {
        val program = for {
          repo       <- ZIO.service[CollectionRepository]
          collection <- repo.create(collection1)
        } yield collection

        assertZIO(program)(Assertion.assertion("checking the created collection") { collection =>
          collection.id == 1L
        })
      },
      test("get a collection by id and slug") {
        val program = for {
          repo             <- ZIO.service[CollectionRepository]
          collection       <- repo.create(collection1)
          collectionById   <- repo.getById(collection.id)
          collectionBySlug <- repo.getBySlug(collection.slug)
        } yield (collection, collectionById, collectionBySlug)

        assertZIO(program)(
          Assertion.assertion("checking that we could fetch a collection by id and slug") {
            case (collection, collectionById, collectionBySlug) =>
              collectionById.contains(collection) &&
              collectionBySlug.contains(collection)
          }
        )

      },
      test("get all collections") {
        val program = for {
          repo           <- ZIO.service[CollectionRepository]
          collections    <- ZIO.collectAll(List(collection1, collection2).map(repo.create))
          allCollections <- repo.getAll
        } yield (collections, allCollections)

        assertZIO(program)(
          Assertion.assertion("checking that the list of collections matches those created") {
            case (collections, allCollections) =>
              collections.toSet == allCollections.toSet
          }
        )
      },
      test("update a collection") {
        val program = for {
          repo              <- ZIO.service[CollectionRepository]
          collection        <- repo.create(collection1)
          updatedCollection <- repo.update(collection.id, updatedCollection1)
        } yield (collection, updatedCollection)

        assertZIO(program)(
          Assertion.assertion("checking that a collection can be updated") {
            case (collection, updatedCollection) =>
              collection.id == updatedCollection.id &&
              updatedCollection.name == updatedCollection1.name &&
              updatedCollection.description == updatedCollection1.description &&
              updatedCollection.yearStart == updatedCollection1.yearStart &&
              updatedCollection.yearEnd == updatedCollection1.yearEnd &&
              updatedCollection.slug == collection1.slug
          }
        )
      },
      test("delete a collection that exists, and fails on one that doesn't exist") {
        val program = for {
          repo       <- ZIO.service[CollectionRepository]
          collection <- repo.create(collection1)

          // Should return true when successfully deleting a collection
          isCollectionDeleted <- repo.delete(collection.id)

          // Should return false when it fails to delete a collection
          shouldFailDeletingUnknownCollection <- repo.delete(collection.id + 1L)

          // Check that the collection no longer exists in the DB
          checkById <- repo.getById(collection.id)
        } yield (isCollectionDeleted, shouldFailDeletingUnknownCollection, checkById.isEmpty)

        assertZIO(program)(
          Assertion.assertion("checking that a collection can be deleted") {
            _ == (true, false, true)
          }
        )
      }
    ).provide(CollectionRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
