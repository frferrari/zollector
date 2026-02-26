package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.Email
import com.zollector.marketplace.http.requests.LoginRequest
import org.scalajs.dom
import frontroute.*
import zio.*

object LoginPage {

  case class State(
      email: String = "",
      password: String = "",
      upstreamError: Option[String] = None,
      showStatus: Boolean = false
  ) {
    val emailFormatError: Option[String] =
      Option.when(!email.matches(Constants.emailRegex))("Email is invalid")

    val passwordError: Option[String] =
      Option.when(password.isEmpty)("Password can't be empty")

    val errorList  = List(emailFormatError, passwordError, upstreamError)
    val maybeError = errorList.find(_.isDefined).flatten.filter(_ => showStatus)
    val hasErrors  = errorList.exists(_.isDefined)
  }

  val stateVar = Var(State())

  val submitter = Observer[State] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(_.user.loginEndpoint(LoginRequest(Email(state.email), state.password)))
        .map { userToken =>
          stateVar.set(State())
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

  def apply() = div(
    cls := "row",
    div(
      cls := "col-md-5 p-0",
      div(cls := "logo"),
      img(
        src := Constants.logoImage,
        alt := "Zollector"
      )
    ),
    div(
      cls := "col-md-7",
      div(
        cls := "form-section",
        div(cls := "top-section", h1(span("Log In"))),
        children <-- stateVar.signal.map(_.maybeError).map(_.map(renderError)).map(_.toList),
        maybeRenderSuccess(),
        form(
          nameAttr := "signin",
          cls      := "form",
          idAttr   := "form",
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
      )
    )
  )

  def maybeRenderSuccess(shouldShow: Boolean = false) = {
    if (shouldShow) {
      div(
        cls := "page-status-success",
        ""
      )
    } else {
      div()
    }
  }

  def renderError(error: String) = {
    div(cls := "page-status-errors", error)
  }

  def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      plcholder: String,
      updFn: (State, String) => State
  ) =
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type`      := kind,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := plcholder,
            onInput.mapToValue --> stateVar.updater(updFn)
          )
        )
      )
    )

}
