package com.zollector.marketplace.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.domain.data.Collection

trait CollectionRepository {
  def create(collection: Collection): Task[Collection]
  def getById(id: Long, userId: Long): Task[Option[Collection]]
  def getBySlug(slug: String, userId: Long): Task[Option[Collection]]
  def getAll(userId: Long): Task[List[Collection]]
  def updateById(id: Long, userId: Long, collection: Collection): Task[Option[Collection]]
  def updateBySlug(slug: String, userId: Long, collection: Collection): Task[Option[Collection]]
  def deleteById(id: Long, userId: Long): Task[Boolean]
  def deleteBySlug(slug: String, userId: Long): Task[Boolean]
}

class CollectionRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends CollectionRepository {

  import quill.*
  inline given schema: SchemaMeta[Collection]  = schemaMeta[Collection]("collections")
  inline given insMeta: InsertMeta[Collection] = insertMeta[Collection](_.id, _.createdAt, _.updatedAt)
  inline given updMeta: UpdateMeta[Collection] = updateMeta[Collection](_.id, _.createdAt, _.updatedAt)

  override def create(collection: Collection): Task[Collection] =
    run {
      query[Collection]
        .insertValue(lift(collection))
        .returning(c => c)
    }

  override def getById(id: Long, userId: Long): Task[Option[Collection]] =
    run {
      query[Collection].filter(r => r.id == lift(id) && r.userId == lift(userId))
    }.map(_.headOption)

  override def getBySlug(slug: String, userId: Long): Task[Option[Collection]] =
    run {
      query[Collection].filter(r => r.slug == lift(slug) && r.userId == lift(userId))
    }.map(_.headOption)

  override def getAll(userId: Long): Task[List[Collection]] =
    run(query[Collection].filter(_.userId == lift(userId)))

  override def updateById(id: Long, userId: Long, collection: Collection): Task[Option[Collection]] =
    run {
      query[Collection]
        .filter(c => c.id == lift(id) && c.userId == lift(userId))
        .updateValue(lift(collection))
        .returningMany(c => c)
    }.map(_.headOption)

  override def updateBySlug(slug: String, userId: Long, collection: Collection): Task[Option[Collection]] =
    run {
      query[Collection]
        .filter(c => c.slug == lift(slug) && c.userId == lift(userId))
        .updateValue(lift(collection))
        .returningMany(c => c)
    }.map(_.headOption)

  override def deleteById(id: Long, userId: Long): Task[Boolean] =
    run {
      query[Collection]
        .filter(c => c.id == lift(id) && c.userId == lift(userId))
        .delete
    }.map(deleteCount => deleteCount > 0)

  override def deleteBySlug(slug: String, userId: Long): Task[Boolean] =
    run {
      query[Collection]
        .filter(c => c.slug == lift(slug) && c.userId == lift(userId))
        .delete
    }.map(deleteCount => deleteCount > 0)
}

object CollectionRepositoryLive {
  val layer = ZLayer {
    ZIO
      .service[Quill.Postgres[SnakeCase.type]]
      .map(quill => CollectionRepositoryLive(quill))
  }
}
