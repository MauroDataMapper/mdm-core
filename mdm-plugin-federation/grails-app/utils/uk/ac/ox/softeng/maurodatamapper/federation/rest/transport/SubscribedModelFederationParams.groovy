/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.federation.rest.transport

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImporterProviderServiceData
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModel

import grails.validation.Validateable

class SubscribedModelFederationParams implements Validateable {
    SubscribedModel subscribedModel
    String url
    String contentType
    ImporterProviderServiceData importerProviderService

    static constraints = {
        url nullable: true, blank: false
        contentType nullable: true, blank: false
        importerProviderService nullable: true
    }
}
