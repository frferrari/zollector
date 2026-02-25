package com.zollector.marketplace.services

import zio.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.queries.{CollectionFacets, CollectionFilter}
import com.zollector.marketplace.repositories.referential.CategoryRepository

trait CollectionService {
  def create(cmd: CreateCollectionCommand): Task[Collection]
  def getAll(userId: UserId): Task[List[Collection]]
  def getAllTest: Task[List[Collection]]
  def getById(id: CollectionId, userId: UserId): Task[Option[Collection]]
  def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]]
  def updateById(id: CollectionId, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]]
  def updateBySlug(slug: Slug, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]]
  def deleteById(id: CollectionId, userId: UserId): Task[Boolean]
  def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean]
  def allFacets(languageCode: LanguageCode = LanguageCode.EN): Task[CollectionFacets]
  def search(filter: CollectionFilter): Task[List[Collection]]
}

class CollectionServiceLive private (collectionRepo: CollectionRepository, categoryRepo: CategoryRepository)
    extends CollectionService {

  override def create(cmd: CreateCollectionCommand): Task[Collection] =
    collectionRepo.create(cmd.toCollection())

  override def getAll(userId: UserId): Task[List[Collection]] =
    collectionRepo.getAll(userId)

  override def getAllTest: Task[List[Collection]] =
    collectionRepo.getAllTest

  override def getById(id: CollectionId, userId: UserId): Task[Option[Collection]] =
    collectionRepo.getById(id, userId)

  override def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]] =
    collectionRepo.getBySlug(slug, userId)

  override def updateById(id: CollectionId, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    collectionRepo.updateById(id, userId, cmd.toCollection())

  override def updateBySlug(slug: Slug, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    collectionRepo.updateBySlug(slug, userId, cmd.toCollection())

  override def deleteById(id: CollectionId, userId: UserId): Task[Boolean] =
    collectionRepo.deleteById(id, userId)

  override def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean] =
    collectionRepo.deleteBySlug(slug, userId)

  override def allFacets(languageCode: LanguageCode = LanguageCode.EN): Task[CollectionFacets] =
    for {
      categories <- categoryRepo.getAllLocalized(languageCode)
    } yield CollectionFacets(categories)

  override def search(filter: CollectionFilter): Task[List[Collection]] =
    collectionRepo.search(filter)

}

object CollectionServiceLive {
  val layer = ZLayer {
    for {
      collectionRepo <- ZIO.service[CollectionRepository]
      categoryRepo   <- ZIO.service[CategoryRepository]
    } yield new CollectionServiceLive(collectionRepo, categoryRepo)
  }
}
