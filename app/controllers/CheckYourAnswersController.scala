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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.requests.DataRequest
import pages.QuestionPage
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Reads
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.language.LanguageUtils
import viewmodels.checkAnswers.{EndDateSummary, FromDateSummary, PostCodeSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            languageUtils: LanguageUtils,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val messages: Messages = controllerComponents.messagesApi.preferred(request)
      val list = SummaryListViewModel(
         rows = Seq(
           PostCodeSummary.row(request.userAnswers)(messages),
           FromDateSummary.row(request.userAnswers)(messages),
           EndDateSummary.row(request.userAnswers)(messages)).flatten
      )
      Ok(view(list)(request, messages))
  }

  private def makeRow[A](route: Call, page: QuestionPage[A], key: String)(implicit
                        request: DataRequest[AnyContent], reads: Reads[A]
  ): SummaryListRow = {
    val answer = request.userAnswers.get(page)
    val actions = {
      Seq(
        route -> messagesApi.messages
          .get(languageUtils.getCurrentLang.language)
          .flatMap(c => c.get("site.change"))
          .getOrElse("")
      )
    }

    SummaryListRow(
      key = Key(
        content = Text(
          messagesApi.messages
            .get(languageUtils.getCurrentLang.language)
            .flatMap(c=>c.get(s"$key.checkYourAnswersLable"))
            .getOrElse("")
        ),
        classes = "govuk-!-width-two-thirds"
      ),
      value = Value(
        content = Text(answer.getOrElse(0).toString),
        classes = "govuk-!-width-two-quarter"
      ),
      actions = Some (
        Actions (
          items = actions.map { case (call, linkText) =>
              ActionItem (href = call.url, content = Text(linkText), visuallyHiddenText =  None)
           }
        )
      )
    )
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) {
      Redirect(routes.CarbonIntensityController.onPageLoad())
  }

}
