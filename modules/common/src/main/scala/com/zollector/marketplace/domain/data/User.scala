package com.zollector.marketplace.domain.data

import java.time.Instant

case class User(
    id: Long,
    nickname: String,
    email: String,
    firstName: String,
    lastName: String,
    hashedPassword: String,
    createdAt: Instant,
    updatedAt: Option[Instant] = None
) {
  def toUserID: UserID = UserID(id, email)
}

final case class UserID(id: Long, email: String)
