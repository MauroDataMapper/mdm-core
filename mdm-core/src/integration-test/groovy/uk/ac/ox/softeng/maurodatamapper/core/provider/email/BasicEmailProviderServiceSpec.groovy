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
package uk.ac.ox.softeng.maurodatamapper.core.provider.email

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * @since 08/10/2019
 */
@Integration
class BasicEmailProviderServiceSpec extends MdmSpecification implements JsonComparer {

    MessageSource messageSource

    BasicEmailProviderService basicEmailProviderService

    void 'Confirm service info'() {
        expect:
        basicEmailProviderService.version == '2.0'
        basicEmailProviderService.providerType == 'EmailProvider'
        basicEmailProviderService.name == 'BasicEmailProviderService'
        basicEmailProviderService.displayName == 'Basic Email Provider'
        basicEmailProviderService.namespace == 'uk.ac.ox.softeng.maurodatamapper.core.provider.email'
    }

    void "Test sending email, will fail without credentials"() {

        when:
        def res = basicEmailProviderService.sendEmail('MDM', 'mdm@mdm.com', ['Ollie': 'ollie.freeman@gmail.com'], [:], 'Test', 'Hello')

        then:
        res instanceof String
        res.contains('error')
    }

}
