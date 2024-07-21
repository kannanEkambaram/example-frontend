package generators

import models._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  implicit lazy val arbitraryPlaceOfBusiness: Arbitrary[PlaceOfBusiness] =
    Arbitrary {
      Gen.oneOf(PlaceOfBusiness.values.toSeq)
    }
}
