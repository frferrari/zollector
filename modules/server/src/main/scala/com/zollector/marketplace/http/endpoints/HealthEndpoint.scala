package com.zollector.marketplace.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {
  val healthEndpoint =
    endpoint
      .tag("health")
      .name("health")
      .description("Health Check")
      .get
      .in("health")
      .out(plainBody[String])
}
