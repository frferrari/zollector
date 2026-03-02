package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.components.Anchors
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.*
import zio.*

case class ForgotPasswordFormState(
    email: Email = Email(""),
    upstreamError: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  override val errorList: List[Option[String]] = List(
    Option.when(!email.value.matches(Constants.emailRegex))("Email is invalid")
  ) ++ upstreamError.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] =
    upstreamError.flatMap(_.toOption)
}

object ForgotPasswordPage extends FormPage[ForgotPasswordFormState]("Forgot Password") {

  override def basicState = ForgotPasswordFormState()

  def submitter = Observer[ForgotPasswordFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.forgotPasswordEndpoint(ForgotPasswordRequest(state.email)))
        .map { _ =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamError = Some(Right("Check your email inbox and click on the link to recover your password"))
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
      button(
        `type` := "button",
        "Recover Password",
        onClick.preventDefault.mapTo(stateVar.now()) --> submitter
      ),
      Anchors.renderNavLink(
        "Have a password recovery token ?",
        "/recover",
        "auth-link"
      )
    )
}
