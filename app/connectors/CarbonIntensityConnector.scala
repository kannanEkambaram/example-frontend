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

package connectors

import config.FrontendAppConfig
import play.api.http.Status.OK
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CarbonIntensityConnector @Inject() (ws: WSClient,
                                          appConfig: FrontendAppConfig,
                                         )(implicit ec: ExecutionContext)
  extends Logging {

  def getCarbonIntensity(from: String, to: String, postcode: String): Future[Either[String, (String, Double, Double, Double)]] = {

    val url = s"${appConfig.serviceUrl}/$from/$to/postcode/$postcode"
    ws.url(url).get().map { response =>
      response.status match {
        case OK =>
                if(response.json.toString().equals("null")) {
                  Left("No Data Exists")
                } else {
                  Right(getGenerationMixData(response))
                }
        case _ =>
            val jsonObject = Json.parse(response.json.toString())
            val errorMessage = (jsonObject \ "error" \ "message").asOpt[JsValue].getOrElse("")
            Left(s"Failed to fetch carbon intensity data $errorMessage")
      }
    }.recover {
      case e: Exception => Left(s"Exception occurred: ${e.getMessage}")
    }
  }

  private def getGenerationMixData(response: WSResponse): (String, Double, Double, Double) = {
    val jsonObject = Json.parse(response.json.toString())
    val dataArray = (jsonObject \ "data" \ "data" ).asOpt[Seq[JsValue]].getOrElse(Seq.empty[JsValue])

    val totalPercentageMap: Map[String, Double] = dataArray.flatMap { data =>
        val generationMix = (data \ "generationmix").asOpt[Seq[JsValue]].getOrElse(Seq.empty[JsValue])

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
      "renewable"     -> renewablePercentage,
      "nuclear"       -> nuclearPercentage,
      "non-renewable" -> nonRenewablePercentage
    ).maxBy(_._2)._1

    (highestEnergySource, renewablePercentage, nuclearPercentage, nonRenewablePercentage)
  }

}