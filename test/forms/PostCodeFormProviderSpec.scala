package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class PostCodeFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "postCode.error.required"
  val lengthKey = "postCode.error.length"
  val maxLength = 8

  val form = new PostCodeFormProvider()()

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
