package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class PlaceOfBusinessSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "PlaceOfBusiness" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(PlaceOfBusiness.values.toSeq)

      forAll(gen) {
        placeOfBusiness =>

          JsString(placeOfBusiness.toString).validate[PlaceOfBusiness].asOpt.value mustEqual placeOfBusiness
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!PlaceOfBusiness.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[PlaceOfBusiness] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(PlaceOfBusiness.values.toSeq)

      forAll(gen) {
        placeOfBusiness =>

          Json.toJson(placeOfBusiness) mustEqual JsString(placeOfBusiness.toString)
      }
    }
  }
}
