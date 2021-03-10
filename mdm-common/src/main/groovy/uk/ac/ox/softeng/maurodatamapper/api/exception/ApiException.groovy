/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import io.micronaut.http.HttpStatus

import java.time.LocalDateTime

/**
 * Created by james on 27/04/2017.
 */
@SuppressFBWarnings('RANGE_ARRAY_INDEX')
abstract class ApiException extends Exception {

    LocalDateTime dateThrown
    String errorCode
    HttpStatus status

    ApiException(String errorCode, String message) {
        super(message)
        this.errorCode = errorCode
        dateThrown = LocalDateTime.now()
        this.status = HttpStatus.INTERNAL_SERVER_ERROR
    }

    ApiException(String errorCode, String message, Throwable cause) {
        super(message, cause)
        this.errorCode = errorCode
        this.dateThrown = LocalDateTime.now()
    }

    @Override
    String getMessage() {
        cause ? "${super.getMessage()}: ${cause.getMessage()}" : super.getMessage()

    }
}
