package com.zollector.marketplace.domain.errors

abstract class ApplicationException(message: String) extends RuntimeException(message)

case object UnauthorizedException extends ApplicationException("Unauthorized")
