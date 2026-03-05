package com.zollector.marketplace.pages

import com.raquo.laminar.api.L.{*, given}
import com.zollector.marketplace.core.*
import com.zollector.marketplace.core.ZioLaminar.*
import com.zollector.marketplace.domain.data.ValueObjects.*
import com.zollector.marketplace.http.requests.CreateCollectionRequest
import org.scalajs.dom.*
import zio.*

import scala.util.Try

case class CreateCollectionFormState(
    categoryId: CategoryId = CategoryId(0L), // TODO Implement this when we have Flyway
    familyId: FamilyId = FamilyId(0L),       // TODO Implement this when we have Flyway
    name: String = "",
    description: String = "",
    yearStart: Option[String] = None,
    yearEnd: Option[String] = None,
    image: Option[String] = None,
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {
  private val nameFormatError: Option[String] =
    Option.when(name.isEmpty)("Collection name can't be empty")

  private val descriptionFormatError: Option[String] =
    Option.when(description.isEmpty)("Collection description can't be empty")

  private val yearFormatError: Option[String] =
    Option.when(yearCheck)("The range of year start and year end is invalid")

  private val yearCheck: Boolean = {
    val ys = yearStart.flatMap(y => Try(y.trim.toInt).toOption)
    val ye = yearEnd.flatMap(y => Try(y.trim.toInt).toOption)

    (ys, ye) match {
      case (Some(s), Some(e)) if s > e => true
      case (Some(s), _) if s < 1700    => true
      case _                           => false
    }
  }

  private val categoryFormatError: Option[String] =
    Option.when(categoryId.value == -1L)("Collection category can't be empty")

  private val familyFormatError: Option[String] =
    Option.when(familyId.value == -1L)("Collection family can't be empty")

  override val errorList: List[Option[String]] = List(
    nameFormatError,
    descriptionFormatError,
    yearFormatError,
    categoryFormatError,
    familyFormatError
  ) ++ upstreamStatus.map(_.left.toOption).toList

  override val maybeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)
}

object CreateCollectionPage extends FormPage[CreateCollectionFormState]("Create a New Collection") {

  override def basicState = CreateCollectionFormState()

  private def renderCollectionPictureUpload(name: String, uid: String, isRequired: Boolean = false) =
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
          div(
            cls := "image-upload",
            input(
              `type` := "file",
              cls    := "form-control",
              idAttr := uid,
              accept := "image/*",
              onChange.mapToFiles --> fileUploader
            ),
            img(
              cls := "image-upload-thumbnail",
              src <-- stateVar.signal.map(_.image.getOrElse(""))
            )
          )
        )
      )
    )

  val fileUploader = (files: List[File]) => {
    files.headOption
      .filter(_.size > 0)
      .foreach { file =>
        val reader = new FileReader
        reader.onload = _ => {
          val fakeImage = document.createElement("img").asInstanceOf[HTMLImageElement]
          fakeImage.addEventListener(
            "load",
            _ => {
              val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
              val context         = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
              val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
              canvas.width = width
              canvas.height = height
              context.drawImage(fakeImage, 0, 0, width, height)
              stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
            }
          )
          fakeImage.src = reader.result.toString
        }

        reader.readAsDataURL(file)
      }
  }

  private def computeDimensions(width: Int, height: Int): (Int, Int) =
    if (width >= height) {
      val ratio     = width * 1.0 / 256
      val newWidth  = width / ratio
      val newHeight = height / ratio

      (newWidth.toInt, newHeight.toInt)
    } else {
      val (newH, newW) = computeDimensions(height, width)
      (newW, newH)
    }

  val submitter = Observer[CreateCollectionFormState] { state =>
    if (state.hasErrors) {
      stateVar.update(_.copy(showStatus = true))
    } else {
      useBackend(
        _.collection.createEndpoint(
          CreateCollectionRequest(
            state.categoryId,
            state.familyId,
            state.name,
            state.description,
            state.yearStart.map(_.toInt),
            state.yearEnd.map(_.toInt)
          )
        )
      )
        .map { collection =>
          stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Right("Collection created!"))))
        }
        .tapError { e =>
          ZIO.succeed {
            stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
          }
        }
        .runJS
    }
  }

  override def renderChildren() = List(
    renderInput(
      "Name",
      "name-input",
      "text",
      true,
      "Collection Name",
      (s, n) => s.copy(name = n, showStatus = false)
    ),
    renderInput(
      "Description",
      "description-input",
      "text",
      true,
      "Collection Description",
      (s, d) => s.copy(description = d, showStatus = false)
    ),
    renderInput(
      "Year Start",
      "ys-input",
      "number",
      false,
      "Collection starting year",
      (s, ys) => s.copy(yearStart = Some(ys), showStatus = false)
    ),
    renderInput(
      "Year End",
      "ye-input",
      "number",
      false,
      "Collection end year",
      (s, ye) => s.copy(yearEnd = Some(ye), showStatus = false)
    ),
    renderCollectionPictureUpload("Collection Picture", "collection-picture-input", true),
    button(
      `type` := "button",
      "Create Collection",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

}
