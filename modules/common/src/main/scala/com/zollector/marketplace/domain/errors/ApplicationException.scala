package com.zollector.marketplace.domain.errors

abstract class ApplicationException(message: String) extends RuntimeException(message)

case class UnauthorizedException(msg: String) extends ApplicationException(msg)
