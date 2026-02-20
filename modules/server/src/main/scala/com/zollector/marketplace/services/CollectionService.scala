package com.zollector.marketplace.services

import zio.*

import com.zollector.marketplace.domain.commands.*
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.repositories.CollectionRepository

trait CollectionService {
  def create(cmd: CreateCollectionCommand): Task[Collection]
  def getAll(userId: Long): Task[List[Collection]]
  def getById(id: Long, userId: Long): Task[Option[Collection]]
  def getBySlug(slug: String, userId: Long): Task[Option[Collection]]
  def updateById(id: Long, userId: Long, cmd: UpdateCollectionCommand): Task[Option[Collection]]
  def updateBySlug(slug: String, userId: Long, cmd: UpdateCollectionCommand): Task[Option[Collection]]
  def deleteById(id: Long, userId: Long): Task[Boolean]
  def deleteBySlug(slug: String, userId: Long): Task[Boolean]
}

class CollectionServiceLive private (repo: CollectionRepository) extends CollectionService {

  override def create(cmd: CreateCollectionCommand): Task[Collection] =
    repo.create(cmd.toCollection())

  override def getAll(userId: Long): Task[List[Collection]] =
    repo.getAll(userId)

  override def getById(id: Long, userId: Long): Task[Option[Collection]] =
    repo.getById(id, userId)

  override def getBySlug(slug: String, userId: Long): Task[Option[Collection]] =
    repo.getBySlug(slug, userId)

  override def updateById(id: Long, userId: Long, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    repo.updateById(id, userId, cmd.toCollection())

  override def updateBySlug(slug: String, userId: Long, cmd: UpdateCollectionCommand): Task[Option[Collection]] =
    repo.updateBySlug(slug, userId, cmd.toCollection())

  override def deleteById(id: Long, userId: Long): Task[Boolean] =
    repo.deleteById(id, userId)

  override def deleteBySlug(slug: String, userId: Long): Task[Boolean] =
    repo.deleteBySlug(slug, userId)
}

object CollectionServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CollectionRepository]
    } yield new CollectionServiceLive(repo)
  }
}
