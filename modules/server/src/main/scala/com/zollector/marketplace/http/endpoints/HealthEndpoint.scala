package com.zollector.marketplace.http.endpoints

import sttp.tapir.*

trait HealthEndpoint extends BaseEndpoint {
  val healthEndpoint =
    baseEndpoint
      .tag("health")
      .name("health")
      .description("Health Check")
      .get
      .in("health")
      .out(plainBody[String])
}
