package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.useBackend
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.*
import org.scalajs.dom
import frontroute.*
import zio.*

case class ChangePasswordFormState(
    currentPassword: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamError: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] = List(
    Option.when(currentPassword.isEmpty)("Password can't be empty"),
    Option.when(newPassword.isEmpty)("New Password can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upstreamError.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamError.flatMap(_.toOption)
}

object ProfilePage extends FormPage[ChangePasswordFormState]("Profile") {

  override def basicState = ChangePasswordFormState()

  def submitter(email: Email) = Observer[ChangePasswordFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.user.updatePasswordEndpoint(
          UpdatePasswordRequest(email, state.currentPassword, state.newPassword)
        )
      )
        .map { userResponse =>
          stateVar.update(_.copy(showStatus = true, upstreamError = Some(Right("Password successfully changed"))))
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
    Session.getUserState
      .map(_.email)
      .map(email =>
        List(
          renderInput(
            "Password",
            "current-password-input",
            "password",
            true,
            "Your current password",
            (s, p) => s.copy(currentPassword = p, showStatus = false, upstreamError = None)
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
            onClick.preventDefault.mapTo(stateVar.now()) --> submitter(email)
          )
        )
      )
      .getOrElse(
        List(
          div(cls := "centered-text", "It seems you're not logged in yet")
        )
      )
}
