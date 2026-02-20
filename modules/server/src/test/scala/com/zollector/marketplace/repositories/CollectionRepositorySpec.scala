package com.zollector.marketplace.repositories

import zio.*
import zio.test.*

import javax.sql.DataSource
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.repositories.*

object CollectionRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/collections.sql"

  private val bobUserId    = 1L
  private val michioUserId = 2L

  private val bobCollectionA = CreateCollectionCommand(
    userId = bobUserId,
    name = "Norway 1960 1990",
    description = "Stamps of Norway from 1960 to 1990",
    yearStart = Some(1960),
    yearEnd = Some(1990)
  ).toCollection(1L)

  private val bobCollectionB = CreateCollectionCommand(
    userId = bobUserId,
    name = "Sweden 1940 1970",
    description = "Stamps of Sweden from 1940 to 1970",
    yearStart = Some(1940),
    yearEnd = Some(1970)
  ).toCollection(2L)

  private val michioCollectionA = CreateCollectionCommand(
    userId = michioUserId,
    name = "Finland 1930 1980",
    description = "Stamps of Finland from 1930 to 1980",
    yearStart = Some(1930),
    yearEnd = Some(1980)
  ).toCollection(3L)

  private val bobCollectionAupdated = bobCollectionA.copy(
    name = "Norway 1950 2000",
    description = "Stamps of Norway from 1950 to 2000",
    yearStart = Some(1950),
    yearEnd = Some(2000)
  )

  extension (collection: Collection) {
    def minCreatedAt: Collection = collection.copy(createdAt = java.time.Instant.MIN)
  }

  extension (collections: List[Collection]) {
    def minCreatedAt: List[Collection] = collections.map(c => c.minCreatedAt)
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CollectionRepositorySpec")(
      test("create a collection") {
        for {
          repo       <- ZIO.service[CollectionRepository]
          collection <- repo.create(bobCollectionA)
        } yield assertTrue(collection.id == 1L)
      },
      test("get a collection by id and slug and userId") {
        for {
          repo                     <- ZIO.service[CollectionRepository]
          collection               <- repo.create(bobCollectionA)
          collectionById           <- repo.getById(collection.id, bobCollectionA.userId)
          collectionBySlug         <- repo.getBySlug(collection.slug, bobCollectionA.userId)
          notFoundCollectionById   <- repo.getById(collection.id, -1L)
          notFoundCollectionBySlug <- repo.getBySlug(collection.slug, -1L)
        } yield assertTrue(
          collectionById.contains(collection) &&
            collectionBySlug.contains(collection) &&
            notFoundCollectionById.isEmpty &&
            notFoundCollectionBySlug.isEmpty
        )
      },
      test("get all collections") {
        for {
          repo              <- ZIO.service[CollectionRepository]
          collections       <- ZIO.collectAll(List(bobCollectionA, bobCollectionB, michioCollectionA).map(repo.create))
          bobCollections    <- repo.getAll(bobUserId)
          michioCollections <- repo.getAll(michioUserId)
        } yield assertTrue(
          bobCollections.minCreatedAt.toSet == Set(bobCollectionA.minCreatedAt, bobCollectionB.minCreatedAt) &&
            michioCollections.minCreatedAt.toSet == Set(michioCollectionA.minCreatedAt)
        )
      },
      test("updateById a collection") {
        for {
          repo                  <- ZIO.service[CollectionRepository]
          bobCollectionA        <- repo.create(bobCollectionA)
          bobCollectionB        <- repo.create(bobCollectionB)
          updatedCollectionA    <- repo.updateById(bobCollectionA.id, bobUserId, bobCollectionAupdated)
          fetchedBobCollectionB <- repo.getById(bobCollectionB.id, bobCollectionB.userId)
        } yield assertTrue(
          updatedCollectionA.map(_.id).contains(bobCollectionA.id) &&
            updatedCollectionA.map(_.name).contains(bobCollectionAupdated.name) &&
            updatedCollectionA.map(_.description).contains(bobCollectionAupdated.description) &&
            updatedCollectionA.map(_.yearStart).contains(bobCollectionAupdated.yearStart) &&
            updatedCollectionA.map(_.yearEnd).contains(bobCollectionAupdated.yearEnd) &&
            updatedCollectionA.map(_.slug).contains(bobCollectionA.slug) &&
            //
            fetchedBobCollectionB.map(_.id).contains(bobCollectionB.id) &&
            fetchedBobCollectionB.map(_.name).contains(bobCollectionB.name) &&
            fetchedBobCollectionB.map(_.description).contains(bobCollectionB.description) &&
            fetchedBobCollectionB.map(_.yearStart).contains(bobCollectionB.yearStart) &&
            fetchedBobCollectionB.map(_.yearEnd).contains(bobCollectionB.yearEnd) &&
            fetchedBobCollectionB.map(_.slug).contains(bobCollectionB.slug)
        )
      },
      test("updateBySlug a collection") {
        for {
          repo                  <- ZIO.service[CollectionRepository]
          bobCollectionA        <- repo.create(bobCollectionA)
          bobCollectionB        <- repo.create(bobCollectionB)
          updatedCollectionA    <- repo.updateBySlug(bobCollectionA.slug, bobUserId, bobCollectionAupdated)
          fetchedBobCollectionB <- repo.getById(bobCollectionB.id, bobCollectionB.userId)
        } yield assertTrue(
          updatedCollectionA.map(_.id).contains(bobCollectionA.id) &&
            updatedCollectionA.map(_.name).contains(bobCollectionAupdated.name) &&
            updatedCollectionA.map(_.description).contains(bobCollectionAupdated.description) &&
            updatedCollectionA.map(_.yearStart).contains(bobCollectionAupdated.yearStart) &&
            updatedCollectionA.map(_.yearEnd).contains(bobCollectionAupdated.yearEnd) &&
            updatedCollectionA.map(_.slug).contains(bobCollectionA.slug) &&
            //
            fetchedBobCollectionB.map(_.id).contains(bobCollectionB.id) &&
            fetchedBobCollectionB.map(_.name).contains(bobCollectionB.name) &&
            fetchedBobCollectionB.map(_.description).contains(bobCollectionB.description) &&
            fetchedBobCollectionB.map(_.yearStart).contains(bobCollectionB.yearStart) &&
            fetchedBobCollectionB.map(_.yearEnd).contains(bobCollectionB.yearEnd) &&
            fetchedBobCollectionB.map(_.slug).contains(bobCollectionB.slug)
        )
      },
      test("deleteById a collection that exists, and fails on one that doesn't exist") {
        for {
          repo              <- ZIO.service[CollectionRepository]
          bobCollectionA    <- repo.create(bobCollectionA)
          bobCollectionB    <- repo.create(bobCollectionB)
          michioCollectionA <- repo.create(michioCollectionA)

          // Should return true when successfully deleting a collection
          isCollectionDeleted <- repo.deleteById(bobCollectionA.id, bobUserId)

          // Should return false when it fails to deleteBySlug a collection
          shouldFailDeletingUnknownCollection <- repo.deleteById(-1L, bobUserId).map(r => !r)

          // Check the state of the collections in the DB
          bobCollectionAnoLongerExistsById <- repo.getById(bobCollectionA.id, bobCollectionA.userId).map(_.isEmpty)
          bobCollectionAnoLongExistsBySlug <- repo.getBySlug(bobCollectionA.slug, bobCollectionA.userId).map(_.isEmpty)

          bobCollectionBstillExistsById   <- repo.getById(bobCollectionB.id, bobCollectionB.userId).map(_.nonEmpty)
          bobCollectionBstillExistsBySlug <- repo.getBySlug(bobCollectionB.slug, bobCollectionB.userId).map(_.nonEmpty)

          michioCollectionAstillExistsById <- repo
            .getById(michioCollectionA.id, michioCollectionA.userId)
            .map(_.nonEmpty)
        } yield assertTrue(
          isCollectionDeleted &&
            shouldFailDeletingUnknownCollection &&
            bobCollectionAnoLongerExistsById &&
            bobCollectionAnoLongExistsBySlug &&
            bobCollectionBstillExistsById &&
            bobCollectionBstillExistsBySlug &&
            michioCollectionAstillExistsById
        )
      },
      test("deleteBySlug a collection that exists, and fails on one that doesn't exist") {
        for {
          repo              <- ZIO.service[CollectionRepository]
          bobCollectionA    <- repo.create(bobCollectionA)
          bobCollectionB    <- repo.create(bobCollectionB)
          michioCollectionA <- repo.create(michioCollectionA)

          // Should return true when successfully deleting a collection
          isCollectionDeleted <- repo.deleteBySlug(bobCollectionA.slug, bobUserId)

          // Should return false when it fails to deleteBySlug a collection
          shouldFailDeletingUnknownCollection <- repo.deleteBySlug("none", bobUserId).map(r => !r)

          // Check the state of the collections in the DB
          bobCollectionAnoLongerExistsById <- repo.getById(bobCollectionA.id, bobCollectionA.userId).map(_.isEmpty)
          bobCollectionAnoLongExistsBySlug <- repo.getBySlug(bobCollectionA.slug, bobCollectionA.userId).map(_.isEmpty)

          bobCollectionBstillExistsById   <- repo.getById(bobCollectionB.id, bobCollectionB.userId).map(_.nonEmpty)
          bobCollectionBstillExistsBySlug <- repo.getBySlug(bobCollectionB.slug, bobCollectionB.userId).map(_.nonEmpty)

          michioCollectionAstillExistsById <- repo
            .getById(michioCollectionA.id, michioCollectionA.userId)
            .map(_.nonEmpty)
        } yield assertTrue(
          isCollectionDeleted &&
            shouldFailDeletingUnknownCollection &&
            bobCollectionAnoLongerExistsById &&
            bobCollectionAnoLongExistsBySlug &&
            bobCollectionBstillExistsById &&
            bobCollectionBstillExistsBySlug &&
            michioCollectionAstillExistsById
        )
      }
    ).provide(CollectionRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
