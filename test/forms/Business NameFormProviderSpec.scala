package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class Business NameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "business Name.error.required"
  val lengthKey = "business Name.error.length"
  val maxLength = 15

  val form = new Business NameFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
