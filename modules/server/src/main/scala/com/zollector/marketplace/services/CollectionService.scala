package com.zollector.marketplace.services

import zio.*
import com.zollector.marketplace.http.requests.*
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.repositories.*
import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.ValueObjects.*

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
}

class CollectionServiceLive private (repo: CollectionRepository) extends CollectionService {

  override def create(cmd: CreateCollectionCommand): Task[Collection] =
    repo.create(cmd.toCollection())

  override def getAll(userId: UserId): Task[List[Collection]] =
    repo.getAll(userId)

  override def getAllTest: Task[List[Collection]] =
    repo.getAllTest

  override def getById(id: CollectionId, userId: UserId): Task[Option[Collection]] =
    repo.getById(id, userId)

  override def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]] =
    repo.getBySlug(slug, userId)

  override def updateById(id: CollectionId, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    repo.updateById(id, userId, cmd.toCollection())

  override def updateBySlug(slug: Slug, userId: UserId, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    repo.updateBySlug(slug, userId, cmd.toCollection())

  override def deleteById(id: CollectionId, userId: UserId): Task[Boolean] =
    repo.deleteById(id, userId)

  override def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean] =
    repo.deleteBySlug(slug, userId)
}

object CollectionServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CollectionRepository]
    } yield new CollectionServiceLive(repo)
  }
}
