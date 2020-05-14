package uk.ac.ox.softeng.maurodatamapper.api.exception

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * @since 13/02/2018
 */
class ApiInvalidModelException extends ApiException {

    private static final Logger logger = LoggerFactory.getLogger(ApiInvalidModelException)

    Errors errors
    MessageSource messageSource

    ApiInvalidModelException(String errorCode, String message, Errors errors) {
        super(errorCode, message)
        this.errors = errors
    }

    ApiInvalidModelException(String errorCode, String message, Errors errors, MessageSource messageSource) {
        this(errorCode, message, errors)
        this.messageSource = messageSource
    }

    ApiInvalidModelException(String errorCode, String message, Errors errors, Throwable cause) {
        super(errorCode, message, cause)
        this.errors = errors
    }

    @Override
    String getMessage() {
        "${super.getMessage()}\n  ${generateErrorsString()}"
    }

    @SuppressWarnings('SystemErrPrint')
    void outputErrors(MessageSource messageSource) {
        logger.error('Errors validating domain: {}', errors.objectName)
        System.err.println("Errors validating domain: ${errors.objectName}")

        errors.getAllErrors().each {error ->
            String msg = messageSource != null ? messageSource.getMessage(error, Locale.getDefault()) :
                         "${error.getDefaultMessage()} :: ${Arrays.asList(error.getArguments())}"

            if (error instanceof FieldError) msg += " :: [${((FieldError) error).getField()}]"

            logger.error(msg)
            System.err.println(msg)
        }
    }

    String generateErrorsString() {
        errors.getAllErrors().collect {error ->
            String msg = messageSource != null ? messageSource.getMessage(error, Locale.getDefault()) :
                         "${error.getDefaultMessage()} :: ${Arrays.asList(error.getArguments())}"

            if (error instanceof FieldError) msg += " :: [${((FieldError) error).getField()}]"
            msg
        }.join('\n  ')
    }
}
