package com.zollector.marketplace.domain.data

final case class PasswordRecoveryToken(email: String, token: String, expiration: Long)
