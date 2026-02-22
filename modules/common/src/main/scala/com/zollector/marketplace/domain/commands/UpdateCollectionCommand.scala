package com.zollector.marketplace.domain.commands

import com.zollector.marketplace.domain.data.*
import com.zollector.marketplace.domain.data.ValueObjects.*

import java.time.Instant
import java.util.UUID

case class UpdateCollectionCommand(
    userId: UserId,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
) {
  def toCollection(collectionId: CollectionId = CollectionId.random) =
    Collection(
      id = collectionId,
      userId = userId,
      name = name,
      description = description,
      yearStart = yearStart,
      yearEnd = yearEnd,
      slug = Collection.makeSlug(name),
      image = None,
      createdAt = Instant.now()
    )
}
