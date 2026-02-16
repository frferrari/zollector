package com.zollector.marketplace.http.endpoints

import sttp.tapir.*
import com.zollector.marketplace.domain.errors.HttpError

trait BaseEndpoint {
  val baseEndpoint =
    endpoint
      .errorOut(statusCode and plainBody[String])
      .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)
}
