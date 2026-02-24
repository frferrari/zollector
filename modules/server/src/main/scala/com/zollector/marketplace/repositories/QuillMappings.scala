package com.zollector.marketplace.repositories

import io.getquill.MappedEncoding
import com.zollector.marketplace.domain.data.ValueObjects.*

import java.util.UUID

trait QuillMappings:
  given MappedEncoding[UserId, UUID]                = MappedEncoding(_.value)
  given MappedEncoding[UUID, UserId]                = MappedEncoding(UserId(_))
  given MappedEncoding[CategoryId, Long]            = MappedEncoding(_.value)
  given MappedEncoding[Long, CategoryId]            = MappedEncoding(CategoryId(_))
  given MappedEncoding[CategoryTranslationId, Long] = MappedEncoding(_.value)
  given MappedEncoding[Long, CategoryTranslationId] = MappedEncoding(CategoryTranslationId(_))
  given MappedEncoding[FamilyId, Long]              = MappedEncoding(_.value)
  given MappedEncoding[Long, FamilyId]              = MappedEncoding(FamilyId(_))
  given MappedEncoding[CollectionId, UUID]          = MappedEncoding(_.value)
  given MappedEncoding[UUID, CollectionId]          = MappedEncoding(CollectionId(_))
  given MappedEncoding[Email, String]               = MappedEncoding(_.value)
  given MappedEncoding[String, Email]               = MappedEncoding(Email(_))
  given MappedEncoding[HashedPassword, String]      = MappedEncoding(_.value)
  given MappedEncoding[String, HashedPassword]      = MappedEncoding(HashedPassword(_))
  given MappedEncoding[Slug, String]                = MappedEncoding(_.value)
  given MappedEncoding[String, Slug]                = MappedEncoding(Slug(_))
  given MappedEncoding[LanguageCode, String]        = MappedEncoding(_.value)
  given MappedEncoding[String, LanguageCode]        = MappedEncoding(LanguageCode(_))
