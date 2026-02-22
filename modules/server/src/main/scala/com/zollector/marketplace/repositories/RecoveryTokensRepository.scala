package com.zollector.marketplace.repositories

import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import com.zollector.marketplace.config.{Configs, RecoveryTokensConfig}
import com.zollector.marketplace.domain.data.PasswordRecoveryToken
import com.zollector.marketplace.domain.data.ValueObjects.Email

trait RecoveryTokensRepository {
  def getToken(email: Email): Task[Option[String]]
  def checkToken(email: Email, token: String): Task[Boolean]
}

class RecoveryTokensRepositoryLive private (
    tokenConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository with QuillMappings {

  import quill.*

  inline given schema: SchemaMeta[PasswordRecoveryToken]  = schemaMeta[PasswordRecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()
  inline given updMeta: UpdateMeta[PasswordRecoveryToken] = updateMeta[PasswordRecoveryToken](_.email)

  private val tokenDuration = 600000 // TODO pass this from config

  private def randomUppercaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: Email): Task[Option[String]] =
    run(query[PasswordRecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))

  private def replaceToken(email: Email): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .filter(_.email == lift(email))
          .updateValue(
            lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
          )
          .returning(r => r)
      )
    } yield token

  private def generateToken(email: Email): Task[String] = for {
    token <- randomUppercaseString(8)
    _ <- run(
      query[PasswordRecoveryToken]
        .insertValue(
          lift(PasswordRecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
        )
        .returning(r => r)
    )
  } yield token

  private def makeFreshToken(email: Email): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  override def getToken(email: Email): Task[Option[String]] =
    userRepo.getByEmail(email).flatMap {
      case None               => ZIO.none
      case Some(existingUser) => makeFreshToken(email).map(Some(_))
    }

  override def checkToken(email: Email, token: String): Task[Boolean] =
    run(
      query[PasswordRecoveryToken].filter(r => r.email == lift(email) && r.token == lift(token))
    )
      .map(_.nonEmpty)
}

object RecoveryTokensRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer = Configs.makeLayer[RecoveryTokensConfig](Configs.CONFIG_RECOVERY_TOKENS) >>> layer

}
