package com.zollector.marketplace.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import com.zollector.marketplace.core.ZioLaminar
import com.zollector.marketplace.domain.data.CollectionFilter
import org.scalajs.dom
import zio.*
import ZioLaminar.*

object FilterPanel {
  case class CheckValueEvent(groupName: String, value: String, checked: Boolean)

  val GROUP_CATEGORIES = "Categories"
  // ALT.1 val possibleFilter = Var[CollectionFilter](CollectionFilter.empty)
  // ALT.2 val possibleFilter = EventBus[CollectionFilter]()
  val possibleFilter = Var[CollectionFilter](CollectionFilter.empty)
  val checkEvents    = EventBus[CheckValueEvent]()
  val state: Signal[CollectionFilter] = checkEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, value, checked) =>
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + value))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - value))
      }
    }
    .map { checkMap =>
      val categories        = possibleFilter.now().categories
      val checkedCategories = checkMap.getOrElse(GROUP_CATEGORIES, Set())

      CollectionFilter(
        categories = categories
          .filter(c => checkedCategories.contains(c.name))
          .map(lc => lc.copy(description = ">" + checkedCategories.mkString(", ") + "<"))
      )
    }

  def apply() =
    div(
      // ALT.1 using a Var
      //         onMountCallback(_ => useBackend(_.collection.allFiltersEndpoint(())).map(f => possibleFilter.set(f)).runJS),
      //
      // ALT.2 using a EventBus
      //         onMountCallback(_ => useBackend(_.collection.allFiltersEndpoint(())).emitTo(possibleFilter)),
      onMountCallback(_ => useBackend(_.collection.allFiltersEndpoint(())).map(f => possibleFilter.set(f)).runJS),
      // to help debug the state child.text <-- state.map(_.toString),
      // to help debug           child.text <-- checkEvents.events.map(_.toString),
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
            renderApplyButton()
          )
        )
      )
    )

  def renderApplyButton() = {
    div(
      cls := "jvm-accordion-search-btn",
      button(
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
  }

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
            children <-- possibleFilter.signal.map(filter =>
              optionsFn(filter).map(value => renderCheckbox(groupName, value))
            )
            // ALT.1 using a Var
            // children <-- possibleFilter.signal.map(filter =>
            //   optionsFn(filter).map(value => renderCheckbox(groupName, value))
            // )
            // ALT.2 using a EventBus
            // children <-- possibleFilter.events
            //    .toSignal(CollectionFilter.empty)
            //    .map(filter => optionsFn(filter).map(value => renderCheckbox(groupName, value)))
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
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(checked => CheckValueEvent(groupName, value, checked)) --> checkEvents
      )
    )
}
