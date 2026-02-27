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

case class LoginFormState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid")

  private val passwordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  override val errorList: List[Option[String]] = List(emailFormatError, passwordError, upstreamError)
  override val maybeSuccess: Option[String]    = None
}

object LoginPage extends FormPage[LoginFormState]("Log In") {

  override val stateVar: Var[LoginFormState] = Var(LoginFormState())

  val submitter = Observer[LoginFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.loginEndpoint(LoginRequest(Email(state.email), state.password)))
        .map { userToken =>
          Session.setUserState(userToken)
          stateVar.set(LoginFormState())
          BrowserNavigation.replaceState("/")
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
          }
        }
        .runJS
    }
  }

  override def renderChildren() = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamError = None)
    ),
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamError = None)
    ),
    button(
      `type` := "button",
      "Log In",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )
}
