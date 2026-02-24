package com.zollector.marketplace.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.core.ZioLaminar
import com.zollector.marketplace.domain.data.CollectionFilter
import org.scalajs.dom
import zio.*
import ZioLaminar.*

object FilterPanel {
  val GROUP_CATEGORIES = "Categories"
  // ALT.1 val possibleFilter   = Var[CollectionFilter](CollectionFilter.empty)
  val possibleFilter = EventBus[CollectionFilter]()

  def apply() =
    div(
      // ALT.1 onMountCallback(_ => useBackend(_.collection.allFiltersEndpoint(())).map(f => possibleFilter.set(f)).runJS),
      onMountCallback(_ => useBackend(_.collection.allFiltersEndpoint(())).emitTo(possibleFilter)),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(GROUP_CATEGORIES, _.categories.map(_.name)),
            // renderFilterOptions("Countries", List("UK", "France")),
            // renderFilterOptions("Industries", List("Banking", "Aviation")),
            // renderFilterOptions("Tags", List("Scala", "ZIO", "Typelevel")),
            div(
              cls := "jvm-accordion-search-btn",
              button(
                cls    := "btn btn-primary",
                `type` := "button",
                "Apply Filters"
              )
            )
          )
        )
      )
    )

  def renderFilterOptions(groupName: String, optionsFn: CollectionFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // ALT.1 children <-- possibleFilter.signal.map(filter =>
            //   optionsFn(filter).map(value => renderCheckbox(groupName, value))
            // )
            children <-- possibleFilter.events
              .toSignal(CollectionFilter.empty)
              .map(filter => optionsFn(filter).map(value => renderCheckbox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value"
      )
    )
}
