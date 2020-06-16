/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

import org.grails.datastore.gorm.GormEntity
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.GONE
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED
import static org.springframework.http.HttpStatus.OK

/**
 * @since 02/10/2017
 */
trait MdmController implements UserSecurityPolicyManagerAware {

    abstract Class getResource()

    @Override
    void renderMapForResponse(Map map) {
        respond(map, map.remove('model'))
    }

    void notYetImplemented() {
        render status: NOT_IMPLEMENTED
    }

    void gone() {
        render status: GONE
    }

    void done() {
        render status: OK
    }

    void notFound() {
        notFound(params.id)
    }

    boolean notFound(id) {
        notFound(getResource(), id)
    }

    void errorResponse(HttpStatus status, String message) {
        render status: status, message: message
    }

    Map getMultiErrorResponseMap(List<GormEntity> result) {
        def allModelErrors = result.collect {it.errors}
        def responseMap = [errorCode: 'IMPC01']
        String message
        if (allModelErrors.size() == 1) {
            def mErrors = allModelErrors.first()
            responseMap.errors = mErrors
            message = "The imported model has ${mErrors.errorCount} error${mErrors.errorCount == 1 ? '' : 's'}"
        } else {
            responseMap.errorsList = allModelErrors
            message = "${allModelErrors.size()} of the imported models have errors".toString()
        }
        responseMap.message = message
        responseMap
    }
}