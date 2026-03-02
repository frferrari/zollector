package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.components.Anchors
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.*
import zio.*

case class RecoverPasswordFormState(
    email: Email = Email(""),
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamError: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] = List(
    Option.when(!email.value.matches(Constants.emailRegex))("Email is invalid"),
    Option.when(token.isEmpty)("Token can't be empty"),
    Option.when(newPassword.isEmpty)("Password can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upstreamError.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamError.flatMap(_.toOption)
}

object RecoverPasswordPage extends FormPage[RecoverPasswordFormState]("Recover Password") {

  override def basicState = RecoverPasswordFormState()

  def submitter = Observer[RecoverPasswordFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.recoverPasswordEndpoint(RecoverPasswordRequest(state.email, state.token, state.newPassword))
      )
        .map { _ =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamError = Some(Right("You've successfully changed your password"))
            )
          )
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(Left(e.getMessage))))
          }
        }
        .runJS
    }
  }

  override def renderChildren() =
    List(
      renderInput(
        "Email",
        "email-input",
        "text",
        true,
        "Your email",
        (s, e) => s.copy(email = Email(e), showStatus = false, upstreamError = None)
      ),
      renderInput(
        "Recovery Token (from email)",
        "token-input",
        "text",
        true,
        "Your token",
        (s, t) => s.copy(token = t, showStatus = false, upstreamError = None)
      ),
      renderInput(
        "New Password",
        "new-password-input",
        "password",
        true,
        "New current password",
        (s, p) => s.copy(newPassword = p, showStatus = false, upstreamError = None)
      ),
      renderInput(
        "Confirm New Password",
        "confirm-password-input",
        "password",
        true,
        "Confirm password",
        (s, p) => s.copy(confirmPassword = p, showStatus = false, upstreamError = None)
      ),
      button(
        `type` := "button",
        "Change Password",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      ),
      Anchors.renderNavLink(
        "Need a password recovery token ?",
        "/forgot",
        "auth-link"
      )
    )
}
