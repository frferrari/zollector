package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import zio.*

import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.RegisterUserRequest

case class SignUpFormState(
    nickname: String = "",
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid")

  private val passwordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  private val passwordConfirmationError: Option[String] =
    Option.when(password.isEmpty)("Passwords must match")

  override val errorList: List[Option[String]] =
    List(emailFormatError, passwordError, passwordConfirmationError) ++ upstreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)
}

object SignupPage extends FormPage[SignUpFormState]("Sign Up") {
  override def basicState = SignUpFormState()

  val submitter = Observer[SignUpFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.registerUserEndpoint(
          RegisterUserRequest(
            nickname = state.nickname,
            email = Email(state.email),
            password = state.password,
            firstName = state.firstName,
            lastName = state.lastName
          )
        )
      )
        .map { userResponse =>
          stateVar.update(
            _.copy(showStatus = true, upstreamStatus = Some(Right("Account Created, you can now log in!")))
          )
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
          }
        }
        .runJS
    }
  }
  override def renderChildren(): List[ReactiveHtmlElement[html.Element]] = List(
    renderInput(
      "Nickname",
      "nickname-input",
      "text",
      true,
      "Choose a nickname",
      (s, n) => s.copy(nickname = n, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confirm password",
      (s, cp) => s.copy(confirmPassword = cp, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Firstname",
      "firstname-input",
      "text",
      true,
      "Your firstname",
      (s, f) => s.copy(firstName = f, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Lastname",
      "lastname-input",
      "text",
      true,
      "Your lastname",
      (s, l) => s.copy(lastName = l, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Sign Up",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

}
