package com.zollector.marketplace.components

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import frontroute.*

import com.zollector.marketplace.pages.*

object Router {
  def apply() =
    mainTag(
      routes(
        div(
          cls := "container-fluid",
          (pathEnd | path("collections")) { // localhost: 1234 or localhost:1234/collections
            CollectionsPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignupPage()
          },
          path("profile") {
            ProfilePage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          path("logout") {
            LogoutPage()
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}
