import org.springframework.http.HttpStatus
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

/**
 * Renders validation errors according to vnd.error: https://github.com/blongden/vnd.error
 */
model {
    Errors errors
}

this.response.status HttpStatus.UNPROCESSABLE_ENTITY

errorsBlock {
    def allErrors = errors.allErrors
    total allErrors.size()
    errors allErrors.collect {ObjectError error -> cleanMessage(this.messageSource.getMessage(error, this.locale))}
}

// Remove the unnecessary FQDN from class entries in the messages
static String cleanMessage(String message) {
    message.replaceAll(/\[class (\w+\.)+(\w+)]/, '[$2]')
}