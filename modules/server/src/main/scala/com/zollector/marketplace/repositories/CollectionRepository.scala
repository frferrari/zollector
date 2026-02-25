package com.zollector.marketplace.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.domain.queries.CollectionFilter

import java.util.UUID

trait CollectionRepository {
  def create(collection: Collection): Task[Collection]
  def getById(id: CollectionId, userId: UserId): Task[Option[Collection]]
  def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]]
  def getAll(userId: UserId): Task[List[Collection]]
  def getAllTest: Task[List[Collection]]
  def updateById(id: CollectionId, userId: UserId, collection: Collection): Task[Option[Collection]]
  def updateBySlug(slug: Slug, userId: UserId, collection: Collection): Task[Option[Collection]]
  def deleteById(id: CollectionId, userId: UserId): Task[Boolean]
  def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean]
  def search(filter: CollectionFilter): Task[List[Collection]]
}

class CollectionRepositoryLive private (quill: Quill.Postgres[SnakeCase])
    extends CollectionRepository
    with QuillMappings {

  import quill.*

  inline given schema: SchemaMeta[Collection]  = schemaMeta[Collection]("collections")
  inline given insMeta: InsertMeta[Collection] = insertMeta[Collection](_.createdAt, _.updatedAt)
  inline given updMeta: UpdateMeta[Collection] = updateMeta[Collection](_.id, _.createdAt, _.updatedAt)

  override def create(collection: Collection): Task[Collection] =
    run {
      query[Collection]
        .insertValue(lift(collection))
        .returning(c => c)
    }

  override def getById(id: CollectionId, userId: UserId): Task[Option[Collection]] =
    run {
      query[Collection].filter(r => r.id == lift(id) && r.userId == lift(userId))
    }.map(_.headOption)

  override def getBySlug(slug: Slug, userId: UserId): Task[Option[Collection]] =
    run {
      query[Collection].filter(r => r.slug == lift(slug) && r.userId == lift(userId))
    }.map(_.headOption)

  override def getAll(userId: UserId): Task[List[Collection]] =
    run(query[Collection].filter(_.userId == lift(userId)))

  override def getAllTest: Task[List[Collection]] =
    run(query[Collection])

  override def updateById(id: CollectionId, userId: UserId, collection: Collection): Task[Option[Collection]] =
    run {
      query[Collection]
        .filter(c => c.id == lift(id) && c.userId == lift(userId))
        .updateValue(lift(collection))
        .returningMany(c => c)
    }.map(_.headOption)

  override def updateBySlug(slug: Slug, userId: UserId, collection: Collection): Task[Option[Collection]] =
    run {
      query[Collection]
        .filter(c => c.slug == lift(slug) && c.userId == lift(userId))
        .updateValue(lift(collection))
        .returningMany(c => c)
    }.map(_.headOption)

  override def deleteById(id: CollectionId, userId: UserId): Task[Boolean] =
    run {
      query[Collection]
        .filter(c => c.id == lift(id) && c.userId == lift(userId))
        .delete
    }.map(deleteCount => deleteCount > 0)

  override def deleteBySlug(slug: Slug, userId: UserId): Task[Boolean] =
    run {
      query[Collection]
        .filter(c => c.slug == lift(slug) && c.userId == lift(userId))
        .delete
    }.map(deleteCount => deleteCount > 0)

  override def search(filter: CollectionFilter): Task[List[Collection]] = {
    if (filter.isEmpty) getAllTest // TODO use getAll by userId
    else
      run {
        query[Collection]
          .filter { collection =>
            liftQuery(filter.categories.toSet).contains(collection.categoryId)
          // liftQuery(filter.families.toSet).contains(FamilyId(1L)) // TODO refactor this when families are introduced
          }
      }
  }

}

object CollectionRepositoryLive {
  val layer = ZLayer {
    ZIO
      .service[Quill.Postgres[SnakeCase.type]]
      .map(quill => CollectionRepositoryLive(quill))
  }
}
