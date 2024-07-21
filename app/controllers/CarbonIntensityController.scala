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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import handlers.ErrorHandler
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import views.html.CalculationResultView
import pages.{EndDatePage, FromDatePage, PostCodePage}
import play.api.libs.json.{JsValue, Json}


class CarbonIntensityController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           identify: IdentifierAction,
                                           languageUtils: LanguageUtils,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           ws: WSClient,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: CalculationResultView,
                                           errorHandler: ErrorHandler
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val postcode: String = request.userAnswers.get(PostCodePage).get
      val startDate: String = request.userAnswers.get(FromDatePage).get.toString + "T00:00Z"
      val endDate: String = request.userAnswers.get(EndDatePage).get.toString + "T23:59Z"

      println(s"post code : $postcode, startDate: $startDate, endDate: $endDate")

      getCarbonIntensity(startDate, endDate, postcode).flatMap {
        case Right(response) =>
          val (highestEnergySource, renewablePercentage, nuclearPercentage, nonRenewablePercentage) = response

          Future.successful(Ok(view(postcode, highestEnergySource, renewablePercentage, nuclearPercentage, nonRenewablePercentage)))

            /*
        case Left(errorMessage) =>
          val pageTitle = "Error"
          val heading = "Error fetching carbon Intensity data"
          val message = s"Failed to fetch certain intensity data: $errorMessage"

          Future.successful {
            InternalServerError(errorHandler.standardErrorTemplate(pageTitle, heading, message))
          }*/
      }
  }


  def getCarbonIntensity(from: String, to: String, postcode: String): Future[Either[String, (String, Double, Double, Double)]] = {

    val url = s"https://api.carbonintensity.org.uk/regional/intensity/$from/$to/postcode/$postcode"

    println("**************")
    println(url)
    println("**************")


    ws.url(url).get().map { response =>
      response.status match {
        case OK => Right(getGenerationMixData(response))
        case _ => Left("Failed to fetch carbon intensity data")
      }
    }.recover {
      case e: Exception => Left(s"Exception occurred: ${e.getMessage}")
    }
  }


  def getGenerationMixData(response: WSResponse): (String, Double, Double, Double) = {
    val jsonObject = Json.parse(response.json.toString())
    val dataArray = (jsonObject \ "data" \ "data" ).asOpt[Seq[JsValue]].getOrElse(Seq.empty[JsValue])

    val totalPercentageMap: Map[String, Double] = dataArray.flatMap { data =>
        val generationMix = (data \ "generationmix").asOpt[Seq[JsValue]].getOrElse(Seq.empty[JsValue])

/*
        println("total values: "+ generationMix.map { mix =>
          val fuel = (mix \ "fuel").as[String]
          val perc = (mix \ "perc").as[Double]
          (fuel, perc)
        }.groupBy(_._1).view.mapValues(_.map(_._2).sum).toMap.values.sum)

*/
        generationMix.map { mix =>
          val fuel = (mix \ "fuel").as[String]
          val perc = (mix \ "perc").as[Double]
          (fuel, perc)
        }
      }.groupBy(_._1)
      .view.mapValues(_.map(_._2).sum)
      .toMap

    def genSourceAndPercentageMap(totalPercentageMap: Map[String, Double]): Map[String, Double] = {
      val totalPercentageSum = totalPercentageMap.values.sum
      totalPercentageMap.view.mapValues(perc => BigDecimal(perc / totalPercentageSum  * 100)
        .setScale(2, BigDecimal.RoundingMode.DOWN).toDouble).toMap
    }

    val sourceAndPercentageMap = genSourceAndPercentageMap(totalPercentageMap)

    val renewableFuels = Seq("wind", "biomass", "hydro", "solar")
    val nuclearFuels = Seq("nuclear")
    val nonRenewableFuels = Seq("coal", "other", "imports", "gas")

    val renewablePercentage = sourceAndPercentageMap.view.filterKeys(renewableFuels.contains).values.sum

    val nuclearPercentage = sourceAndPercentageMap.view.filterKeys(nuclearFuels.contains).values.sum

    val nonRenewablePercentage = sourceAndPercentageMap.view.filterKeys(nonRenewableFuels.contains).values.sum


    val highestEnergySource = Seq(
      "renewable" -> 	renewablePercentage,
      "nuclear" -> nuclearPercentage,
      "non-renewable" -> nonRenewablePercentage
    ).maxBy(_._2)._1

    /*println(s"highestEnergySource: $highestEnergySource, renewablePercentage: $renewablePercentage, nuclearPercentage: $nuclearPercentage, nonRenewablePercentage: $nonRenewablePercentage")
    */(highestEnergySource, renewablePercentage, nuclearPercentage, nonRenewablePercentage)
  }

}

