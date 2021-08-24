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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.EmptyJsonProfileFactory
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class ProfileSpecificationProfileServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ProfileSpecificationProfileService>{

    def setup() {
        mockArtefact(MetadataService)
        mockArtefact(MauroDataMapperServiceProviderService)
    }


    def cleanup() {
    }

    void "Profile correctly loaded"() {
        JsonProfile jsonProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(service)
        expect: "2 fields in the profile specification profile"
            service.profileApplicableForDomains() == ["DataModel"]

            jsonProfile.sections.size() == 1
        jsonProfile.sections[0].fields.size() == 3
    }
}
