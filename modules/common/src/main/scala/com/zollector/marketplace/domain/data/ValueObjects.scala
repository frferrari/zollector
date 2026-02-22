package com.zollector.marketplace.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

import java.util.UUID
import sttp.tapir.*
import sttp.tapir.CodecFormat.TextPlain

object ValueObjects:

  opaque type UserId = UUID
  object UserId:
    def apply(value: UUID): UserId         = value
    def random: UserId                     = UUID.randomUUID()
    extension (id: UserId) def value: UUID = id

    given JsonCodec[UserId] =
      JsonCodec[UUID].transform(
        uuid => UserId(uuid),
        _.value
      )

    given Schema[UserId] = summon[Schema[UUID]]

  opaque type CollectionId = UUID
  object CollectionId:
    def apply(value: UUID): CollectionId         = value
    def random: CollectionId                     = UUID.randomUUID()
    extension (id: CollectionId) def value: UUID = id

    given JsonCodec[CollectionId] =
      JsonCodec[UUID].transform(
        uuid => CollectionId(uuid),
        _.value
      )

    given Schema[CollectionId] = summon[Schema[UUID]]

    given Codec[String, CollectionId, TextPlain] =
      summon[Codec[String, UUID, TextPlain]]
        .mapDecode(uuid => DecodeResult.Value(CollectionId(uuid)))(_.value)

  opaque type Email = String
  object Email:
    def apply(value: String): Email          = value
    extension (e: Email) def value: String   = e

    given JsonCodec[Email] = JsonCodec[String].transform(Email(_), _.value)
    given Schema[Email]    = summon[Schema[String]]

  opaque type HashedPassword = String
  object HashedPassword:
    def apply(value: String): HashedPassword         = value
    extension (hp: HashedPassword) def value: String = hp

    given JsonCodec[HashedPassword] = JsonCodec[String].transform(HashedPassword(_), _.value)
    given Schema[HashedPassword]    = summon[Schema[String]]

  opaque type Slug = String
  object Slug:
    def apply(value: String): Slug         = value
    extension (s: Slug) def value: String  = s

    given JsonCodec[Slug] = JsonCodec[String].transform(Slug(_), _.value)
    given Schema[Slug]    = summon[Schema[String]]
