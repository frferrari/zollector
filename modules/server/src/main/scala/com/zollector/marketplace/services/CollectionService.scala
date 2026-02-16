package com.zollector.marketplace.services

import zio.*

import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.http.requests.CreateCollectionRequest
import com.zollector.marketplace.repositories.CollectionRepository

trait CollectionService {
  def create(req: CreateCollectionRequest): Task[Collection]
  def getAll: Task[List[Collection]]
  def getById(id: Long): Task[Option[Collection]]
  def getBySlug(slug: String): Task[Option[Collection]]
}

class CollectionServiceLive private (repo: CollectionRepository) extends CollectionService {

  override def create(req: CreateCollectionRequest): Task[Collection] =
    repo.create(req.toCollection())

  override def getAll: Task[List[Collection]] =
    repo.getAll

  override def getById(id: Long): Task[Option[Collection]] =
    repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Collection]] =
    repo.getBySlug(slug)
}

object CollectionServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CollectionRepository]
    } yield new CollectionServiceLive(repo)
  }
}
