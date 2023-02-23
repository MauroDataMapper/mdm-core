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
package uk.ac.ox.softeng.maurodatamapper.core.provider.email

import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.email.SendEmailTask
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

    EmailService emailService
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
        given:
        SendEmailTask task = new SendEmailTask(emailService)
            .to('Ollie', 'ollie.freeman@gmail.com')
            .from('MDM', 'mdm@mdm.com')
            .subject('Test')
            .body('Hello')

        when:
        String res = basicEmailProviderService.sendEmail(task)

        then:
        res == 'SMTP server host missing'
    }

}
