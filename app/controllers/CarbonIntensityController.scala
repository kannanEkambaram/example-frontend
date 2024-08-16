/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import connectors.CarbonIntensityConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import pages.{EndDatePage, FromDatePage, PostCodePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CalculationResultView
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class CarbonIntensityController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: CalculationResultView,
                                           errorHandler: ErrorHandler,
                                           carbonIntensityConnector: CarbonIntensityConnector
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val postcode: String = request.userAnswers.get(PostCodePage).get
      val startDate: String = request.userAnswers.get(FromDatePage).get.toString + "T00:00Z"
      val endDate: String = request.userAnswers.get(EndDatePage).get.toString + "T23:59Z"

      carbonIntensityConnector.getCarbonIntensity(startDate, endDate, postcode).flatMap {
        case Right(response) =>
          val (h, r, n, nr) = response
          Future.successful(Ok(view(postcode, h, r, n, nr)))

        case Left(errorMessage) =>
          val pageTitle = "Error"
          val heading = "Error fetching carbon Intensity data"
          val message = s"Failed to fetch certain intensity data: $errorMessage"

          errorHandler.standardErrorTemplate(pageTitle, heading, message).map { r =>
            InternalServerError(r)
          }
      }
  }

}

