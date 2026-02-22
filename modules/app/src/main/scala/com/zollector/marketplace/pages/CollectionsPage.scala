package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.common.Constants
import com.zollector.marketplace.components.Anchors
import com.zollector.marketplace.domain.data.Collection
import com.zollector.marketplace.domain.data.ValueObjects.*
import org.scalajs.dom
import frontroute.*

object CollectionsPage {
  val dummyCollection = Collection(
    CollectionId.random,
    UserId.random,
    "Finland 1960 1990",
    "Stamps from Finland 1960 to 1990 MNH",
    Some(1960),
    Some(1990),
    Slug("finland-1960-1990"),
    None,
    java.time.Instant.now()
  )

  def apply() =
    sectionTag(
      cls := "section-1",
      div(
        cls := "container company-list-hero",
        h1(
          cls := "company-list-title",
          "Rock the JVM Companies Board"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row jvm-recent-companies-body",
          div(
            cls := "col-lg-4",
            div("TODO filter panel here")
          ),
          div(
            cls := "col-lg-8",
            renderCollection(dummyCollection),
            renderCollection(dummyCollection)
          )
        )
      )
    )

  private def renderCollectionPicture(collection: Collection) =
    img(
      cls := "img-fluid",
      src := collection.image.getOrElse(Constants.genericCollection),
      alt := collection.name
    )

  private def renderCollectionPeriod(collection: Collection) =
    (collection.yearStart, collection.yearEnd) match {
      case (Some(ys), Some(ye)) => s"$ys - $ye"
      case (None, Some(ye))     => s"up to $ye"
      case (Some(ys), None)     => s"from $ys"
      case (None, None)         => ""
    }

  private def renderSummary(collection: Collection) =
    div(
      cls := "company-summary",
      div(
        cls := "company-detail",
        p(
          cls := "company-detail-value",
          collection.description
        )
      )
    )

  private def renderAction(collection: Collection) =
    div(
      cls := "jvm-recent-companies-card-btn-apply",
      a(
        href   := "https://todo.com",
        target := "blank",
        button(
          `type` := "button",
          cls    := "btn btn-danger rock-action-btn",
          "View"
        )
      )
    )

  def renderCollection(collection: Collection) =
    div(
      cls := "jvm-recent-companies-cards",
      div(
        cls := "jvm-recent-companies-card-img",
        renderCollectionPicture(collection)
      ),
      div(
        cls := "jvm-recent-companies-card-contents",
        h5(
          Anchors.renderNavLink(
            collection.name,
            s"/collections/${collection.id}",
            "company-title-link"
          )
        ),
        renderSummary(collection)
      ),
      renderAction(collection)
    )
}
