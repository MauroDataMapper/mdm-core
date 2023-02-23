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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

/**
 * @since 24/05/2022
 */
class DefaultJsonProfileProviderService extends JsonProfileProviderService {

    String serviceName
    String servicePackage
    String serviceVersion
    String displayName
    String metadataNamespace
    String jsonResourceFile
    boolean canBeEditedAfterFinalisation
    List<String> profileApplicableForDomains


    DefaultJsonProfileProviderService() {
        canBeEditedAfterFinalisation = false
        profileApplicableForDomains = []
        servicePackage = getClass().getPackage().getName()
        serviceVersion = getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getName() {
        serviceName
    }

    @Override
    String getNamespace() {
        servicePackage
    }

    @Override
    String getVersion() {
        serviceVersion
    }

    Boolean canBeEditedAfterFinalisation() {
        canBeEditedAfterFinalisation
    }

    List<String> profileApplicableForDomains() {
        profileApplicableForDomains
    }
}
