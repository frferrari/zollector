package com.zollector.marketplace.core

import com.raquo.laminar.api.L.{*, given}
import com.zollector.marketplace.domain.data.UserToken
import org.scalajs.dom
import scala.scalajs.js.*

object Session {
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive: Boolean =
    userState.now().nonEmpty

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set(stateName, token)
  }

  def loadUserState(): Unit = {
    // Clears any expired token
    Storage
      .get[UserToken](stateName)
      .filter(_.expires * 1000 <= new Date().getTime())
      .foreach(token => Storage.remove(stateName))

    // Retrieve the userToken (known to be valid)
    userState.set(
      Storage
        .get[UserToken](stateName)
    )
  }

  def clearUserState(): Unit = {
    Storage.remove(stateName)
    userState.set(Option.empty)
  }
}
