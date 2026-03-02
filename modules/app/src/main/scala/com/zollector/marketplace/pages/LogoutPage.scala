package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.LoginRequest
import org.scalajs.dom
import frontroute.*
import zio.*

case class LogoutFormState() extends FormState {
  override val errorList: List[Option[String]] = List()
  override val maybeSuccess: Option[String]    = None
  override val showStatus: Boolean             = false
}

object LogoutPage extends FormPage[LogoutFormState]("Log Out") {

  override val stateVar: Var[LogoutFormState] = Var(LogoutFormState())

  override def renderChildren() = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "centered-text",
      "You've been successfully logged out."
    )
  )
}
