package com.zollector.marketplace.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.domain.data.Collection

trait CollectionRepository {
  def create(collection: Collection): Task[Collection]
  def getById(id: Long): Task[Option[Collection]]
  def getBySlug(slug: String): Task[Option[Collection]]
  def getAll: Task[List[Collection]]
  def update(id: Long, collection: Collection): Task[Collection]
  def delete(id: Long): Task[Boolean]
}

class CollectionRepositoryLive private (quill: Quill.Postgres[SnakeCase])
    extends CollectionRepository {

  import quill.*
  inline given schema: SchemaMeta[Collection] = schemaMeta[Collection]("collections")
  inline given insMeta: InsertMeta[Collection] =
    insertMeta[Collection](_.id, _.createdAt, _.updatedAt)
  inline given upMeta: UpdateMeta[Collection] =
    updateMeta[Collection](_.id, _.createdAt, _.updatedAt)

  override def create(collection: Collection): Task[Collection] =
    run {
      query[Collection]
        .insertValue(lift(collection))
        .returning(c => c)
    }

  override def getById(id: Long): Task[Option[Collection]] =
    run {
      query[Collection].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Collection]] =
    run {
      query[Collection].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def getAll: Task[List[Collection]] =
    run(query[Collection])

  override def update(id: Long, collection: Collection): Task[Collection] =
    run {
      query[Collection]
        .filter(_.id == lift(id))
        .updateValue(lift(collection))
        .returning(c => c)
    }

  override def delete(id: Long): Task[Boolean] =
    run {
      query[Collection]
        .filter(_.id == lift(id))
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
