package com.zollector.marketplace.domain.data

import com.zollector.marketplace.domain.data.ValueObjects.*

import java.time.Instant
import java.util.UUID

case class User(
    id: UserId,
    nickname: String,
    email: Email,
    firstName: String,
    lastName: String,
    hashedPassword: HashedPassword,
    createdAt: Instant,
    updatedAt: Option[Instant] = None
) {
  def toUserIdentifier: UserIdentifier = UserIdentifier(id, email)
}

final case class UserIdentifier(id: UserId, email: Email)
