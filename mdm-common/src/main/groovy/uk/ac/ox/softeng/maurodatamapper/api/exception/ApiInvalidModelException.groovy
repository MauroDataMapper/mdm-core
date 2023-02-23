/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.api.exception

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.validation.FieldError

/**
 * @since 13/02/2018
 */
@SuppressFBWarnings(value = 'SE_BAD_FIELD', justification = 'We need the fields and they can\'t be transient')
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
