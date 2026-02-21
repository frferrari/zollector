package com.zollector.marketplace

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.zollector.marketplace.components.*
import frontroute.LinkHandler

object App {
  val app = div(
    Header(),
    Router()
  ).amend(LinkHandler.bind) // for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(containerNode, app)
  }
}
