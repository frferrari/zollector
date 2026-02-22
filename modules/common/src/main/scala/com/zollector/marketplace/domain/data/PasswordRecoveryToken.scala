package com.zollector.marketplace.domain.data

import com.zollector.marketplace.domain.data.ValueObjects.Email

final case class PasswordRecoveryToken(email: Email, token: String, expiration: Long)
