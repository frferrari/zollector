package com.zollector.marketplace.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import frontroute.*
import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Header {
  def apply() =
    div(
      cls := "container-fluid p-0",
      div(
        cls := "jvm-nav",
        div(
          cls := "container",
          navTag(
            cls := "navbar navbar-expand-lg navbar-light JVM-nav",
            div(
              cls := "container",
              renderLogo(),
              button(
                cls                                         := "navbar-toggler",
                `type`                                      := "button",
                htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
                htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
                htmlAttr("aria-controls", StringAsIsCodec)  := "navbarNav",
                htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
                htmlAttr("aria-label", StringAsIsCodec)     := "Toggle navigation",
                span(cls := "navbar-toggler-icon")
              ),
              div(
                cls    := "collapse navbar-collapse",
                idAttr := "navbarNav",
                ul(
                  cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3",
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )

  @js.native
  @JSImport("url:/static/img/fiery-lava-128-128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
      cls  := "navbar-brand",
      img(
        cls := "home-logo",
        src := logoImage,
        alt := "Zollector"
      )
    )

  private def renderNavLinks() = {
    List(
      renderNavLink("Collections", "/collections"),
      renderNavLink("Log In", "/login"),
      renderNavLink("Sign Up", "/signup")
    )
  }

  private def renderNavLink(text: String, location: String) =
    li(
      cls := "nav-item",
      Anchors.renderNavLink(text, location, "nav-link jvm-item")
    )

}

object Anchors {
  def renderNavLink(text: String, location: String, cssClass: String = "") =
    a(
      href := location,
      cls  := cssClass,
      text
    )
}
