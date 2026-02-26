package com.zollector.marketplace.common

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Constants {
  @js.native
  @JSImport("url:/static/img/fiery-lava-128-128.png", JSImport.Default)
  val logoImage: String = js.native

  @js.native
  @JSImport("url:/static/img/generic_company.png", JSImport.Default)
  val genericCollection: String = js.native

  val emailRegex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"""
}
