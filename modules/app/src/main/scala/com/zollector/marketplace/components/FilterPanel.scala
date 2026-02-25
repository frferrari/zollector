package com.zollector.marketplace.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.core.ZioLaminar
import org.scalajs.dom
import zio.*
import ZioLaminar.*
import com.zollector.marketplace.domain.queries.*

class FilterPanel {
  case class CheckValueEvent(groupName: String, id: String, checked: Boolean)

  private val GROUP_CATEGORIES = "Categories"

  // ALT.1 val possibleFacets = Var[CollectionFilter](CollectionFilter.empty)
  // ALT.2 val possibleFacets = EventBus[CollectionFilter]()

  private val possibleFacets = Var[CollectionFacets](CollectionFacets.empty)
  private val checkEvents    = EventBus[CheckValueEvent]()
  private val clicks         = EventBus[Unit]() // clicks on the apply filters button
  private val state: Signal[CollectionFilter] = checkEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, id, checked) =>
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + id))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - id))
      }
    }
    .map { checkMap =>
      val categories        = possibleFacets.now().categories
      val checkedCategories = checkMap.getOrElse(GROUP_CATEGORIES, Set())

      CollectionFilter(
        categories = categories
          .map(_.id)
          .filter(c => checkedCategories.contains(c.value.toString))
      )
    }

  val triggerFilters: EventStream[CollectionFilter] = clicks.events.withCurrentValueOf(state)

  def apply() =
    div(
      // ALT.1 using a Var
      //         onMountCallback(_ => useBackend(_.collection.allFacetsEndpoint(())).map(f => possibleFacets.set(f)).runJS),
      //
      // ALT.2 using a EventBus
      //         onMountCallback(_ => useBackend(_.collection.allFacetsEndpoint(())).emitTo(possibleFacets)),
      onMountCallback(_ => useBackend(_.collection.allFacetsEndpoint(())).map(f => possibleFacets.set(f)).runJS),
      // to help debug the triggerFilters   child.text <-- triggerFilters.map(_.toString),
      // to help debug the state            child.text <-- state.map(_.toString),
      // to help debug                      child.text <-- checkEvents.events.map(_.toString),
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
            renderFilterOptions(GROUP_CATEGORIES, _.categories.map(c => (c.id.toString, c.name))),
            // renderFilterOptions("Countries", List("UK", "France")),
            // renderFilterOptions("Industries", List("Banking", "Aviation")),
            // renderFilterOptions("Tags", List("Scala", "ZIO", "Typelevel")),
            renderApplyButton()
          )
        )
      )
    )

  def renderApplyButton() = {
    div(
      cls := "jvm-accordion-search-btn",
      button(
        onClick.mapTo(()) --> clicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
  }

  def renderFilterOptions(groupName: String, optionsFn: CollectionFacets => List[(String, String)]) =
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
            children <-- possibleFacets.signal.map(facets =>
              optionsFn(facets).map((id, value) => renderCheckbox(groupName, id, value))
            )
            // ALT.1 using a Var
            // children <-- possibleFacets.signal.map(filter =>
            //   optionsFn(filter).map(value => renderCheckbox(groupName, value))
            // )
            // ALT.2 using a EventBus
            // children <-- possibleFacets.events
            //    .toSignal(CollectionFilter.empty)
            //    .map(filter => optionsFn(filter).map(value => renderCheckbox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, id: String, value: String) =
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
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(checked => CheckValueEvent(groupName, id, checked)) --> checkEvents
      )
    )
}
