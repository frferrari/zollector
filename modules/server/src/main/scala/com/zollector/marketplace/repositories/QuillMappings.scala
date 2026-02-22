package com.zollector.marketplace.repositories

import io.getquill.MappedEncoding
import com.zollector.marketplace.domain.data.ValueObjects.*

import java.util.UUID

trait QuillMappings:
  given MappedEncoding[UserId, UUID]             = MappedEncoding(_.value)
  given MappedEncoding[UUID, UserId]             = MappedEncoding(UserId(_))
  given MappedEncoding[CollectionId, UUID]       = MappedEncoding(_.value)
  given MappedEncoding[UUID, CollectionId]       = MappedEncoding(CollectionId(_))
  given MappedEncoding[Email, String]            = MappedEncoding(_.value)
  given MappedEncoding[String, Email]            = MappedEncoding(Email(_))
  given MappedEncoding[HashedPassword, String]   = MappedEncoding(_.value)
  given MappedEncoding[String, HashedPassword]   = MappedEncoding(HashedPassword(_))
  given MappedEncoding[Slug, String]             = MappedEncoding(_.value)
  given MappedEncoding[String, Slug]             = MappedEncoding(Slug(_))
