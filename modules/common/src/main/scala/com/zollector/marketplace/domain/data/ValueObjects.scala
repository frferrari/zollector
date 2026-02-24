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
    given JsonCodec[UserId]                = JsonCodec[UUID].transform(uuid => UserId(uuid), _.value)
    given Schema[UserId]                   = summon[Schema[UUID]]

  opaque type CategoryId = Long
  object CategoryId:
    def apply(value: Long): CategoryId         = value
    extension (id: CategoryId) def value: Long = id
    given JsonCodec[CategoryId]                = JsonCodec[Long].transform(id => CategoryId(id), _.value)
    given Schema[CategoryId]                   = summon[Schema[Long]]

  opaque type CategoryTranslationId = Long
  object CategoryTranslationId:
    def apply(value: Long): CategoryTranslationId         = value
    extension (id: CategoryTranslationId) def value: Long = id
    given JsonCodec[CategoryTranslationId] = JsonCodec[Long].transform(id => CategoryTranslationId(id), _.value)
    given Schema[CategoryTranslationId]    = summon[Schema[Long]]

  opaque type FamilyId = Long
  object FamilyId:
    def apply(value: Long): FamilyId         = value
    extension (id: FamilyId) def value: Long = id
    given JsonCodec[FamilyId]                = JsonCodec[Long].transform(id => FamilyId(id), _.value)
    given Schema[FamilyId]                   = summon[Schema[Long]]

  opaque type CollectionId = UUID
  object CollectionId:
    def apply(value: UUID): CollectionId         = value
    def random: CollectionId                     = UUID.randomUUID()
    extension (id: CollectionId) def value: UUID = id
    given JsonCodec[CollectionId]                = JsonCodec[UUID].transform(uuid => CollectionId(uuid), _.value)
    given Schema[CollectionId]                   = summon[Schema[UUID]]
    given Codec[String, CollectionId, TextPlain] =
      summon[Codec[String, UUID, TextPlain]].mapDecode(uuid => DecodeResult.Value(CollectionId(uuid)))(_.value)

  opaque type Email = String
  object Email:
    def apply(value: String): Email        = value
    extension (e: Email) def value: String = e
    given JsonCodec[Email]                 = JsonCodec[String].transform(Email(_), _.value)
    given Schema[Email]                    = summon[Schema[String]]

  opaque type HashedPassword = String
  object HashedPassword:
    def apply(value: String): HashedPassword         = value
    extension (hp: HashedPassword) def value: String = hp
    given JsonCodec[HashedPassword]                  = JsonCodec[String].transform(HashedPassword(_), _.value)
    given Schema[HashedPassword]                     = summon[Schema[String]]

  opaque type Slug = String
  object Slug:
    def apply(value: String): Slug        = value
    extension (s: Slug) def value: String = s
    given JsonCodec[Slug]                 = JsonCodec[String].transform(Slug(_), _.value)
    given Schema[Slug]                    = summon[Schema[String]]

  opaque type LanguageCode = String
  object LanguageCode:
    val EN                                         = LanguageCode("en")
    val FR                                         = LanguageCode("fr")
    val ES                                         = LanguageCode("es")
    val IT                                         = LanguageCode("it")
    val DE                                         = LanguageCode("de")
    def apply(value: String): LanguageCode         = value
    extension (lc: LanguageCode) def value: String = lc
    given JsonCodec[LanguageCode]                  = JsonCodec[String].transform(LanguageCode(_), _.value)
    given Schema[LanguageCode]                     = summon[Schema[String]]
