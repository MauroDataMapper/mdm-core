/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
class DefaultDynamicImportJsonProfileProviderService extends DynamicImportJsonProfileProviderService {

    String serviceName
    String serviceVersion
    String displayNamePrefix
    String profileNamespace
    String jsonResourceFile
    List<String> profileApplicableForDomains

    DefaultDynamicImportJsonProfileProviderService() {
        super()
        profileApplicableForDomains = []
        serviceVersion = getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
    }

    @Override
    String getName() {
        serviceName
    }

    @Override
    String getVersion() {
        serviceVersion
    }

    List<String> profileApplicableForDomains() {
        profileApplicableForDomains
    }

    @Override
    DefaultDynamicImportJsonProfileProviderService clone() throws CloneNotSupportedException {
        (super.clone() as DefaultDynamicImportJsonProfileProviderService).tap {
            serviceName = owner.serviceName
            serviceVersion = owner.serviceVersion
            displayNamePrefix = owner.displayNamePrefix
            profileNamespace = owner.profileNamespace
            jsonResourceFile = owner.jsonResourceFile
            profileApplicableForDomains = owner.profileApplicableForDomains
        }
    }
}
