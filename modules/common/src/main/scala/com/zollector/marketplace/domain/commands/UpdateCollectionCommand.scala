package com.zollector.marketplace.domain.commands

import com.zollector.marketplace.domain.data.Collection

import java.time.Instant

case class UpdateCollectionCommand(
    userId: Long,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
) {
  def toCollection(id: Long = -1L) =
    Collection(
      id = id,
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
