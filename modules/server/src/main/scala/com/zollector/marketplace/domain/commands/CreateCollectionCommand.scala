package com.zollector.marketplace.domain.commands

import com.zollector.marketplace.domain.data.Collection

import java.time.Instant

case class CreateCollectionCommand(
    userId: Long,
    name: String,
    description: String,
    yearStart: Option[Int] = None,
    yearEnd: Option[Int] = None
) {
  def toCollection(id: Long = -1L) =
    Collection(id, userId, name, description, yearStart, yearEnd, Collection.makeSlug(name), Instant.now())
}
