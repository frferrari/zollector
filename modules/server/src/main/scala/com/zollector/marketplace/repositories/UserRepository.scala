package com.zollector.marketplace.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.domain.data.*

trait UserRepository {
  def create(user: User): Task[User]
  def getById(id: Long): Task[Option[User]]
  def getByEmail(email: String): Task[Option[User]]
  def getByNickname(nickname: String): Task[Option[User]]
  def update(id: Long, user: User): Task[User]
  def delete(id: Long): Task[Boolean]
}

class UserRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends UserRepository {
  import quill.*
  inline given schema: SchemaMeta[User]  = schemaMeta[User]("users")
  inline given insMeta: InsertMeta[User] = insertMeta[User](_.id, _.createdAt, _.updatedAt)
  inline given updMeta: UpdateMeta[User] = updateMeta[User](_.id, _.createdAt, _.updatedAt)

  override def create(user: User): Task[User] =
    run(query[User].insertValue(lift(user)).returning(u => u))

  override def getById(id: Long): Task[Option[User]] =
    run(query[User].filter(_.id == lift(id))).map(_.headOption)

  override def getByEmail(email: String): Task[Option[User]] =
    run(query[User].filter(_.email == lift(email))).map(_.headOption)

  override def getByNickname(nickname: String): Task[Option[User]] =
    run(query[User].filter(_.nickname == lift(nickname))).map(_.headOption)

  override def update(id: Long, user: User): Task[User] =
    run {
      query[User]
        .filter(_.id == lift(id))
        .updateValue(lift(user))
        .returning(u => u)
    }

  override def delete(id: Long): Task[Boolean] =
    run {
      query[User]
        .filter(_.id == lift(id))
        .delete
    }.map(deleteCount => deleteCount > 0)
}

object UserRepositoryLive {
  val layer = ZLayer {
    ZIO
      .service[Quill.Postgres[SnakeCase.type]]
      .map(quill => new UserRepositoryLive(quill))
  }
}
