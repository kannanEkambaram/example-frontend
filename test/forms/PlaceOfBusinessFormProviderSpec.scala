package forms

import forms.behaviours.OptionFieldBehaviours
import models.PlaceOfBusiness
import play.api.data.FormError

class PlaceOfBusinessFormProviderSpec extends OptionFieldBehaviours {

  val form = new PlaceOfBusinessFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "placeOfBusiness.error.required"

    behave like optionsField[PlaceOfBusiness](
      form,
      fieldName,
      validValues  = PlaceOfBusiness.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
