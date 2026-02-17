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
)
