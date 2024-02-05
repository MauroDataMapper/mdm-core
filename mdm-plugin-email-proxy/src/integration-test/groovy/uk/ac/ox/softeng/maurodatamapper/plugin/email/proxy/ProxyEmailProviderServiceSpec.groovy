/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy

import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.email.SendEmailTask
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

/**
 * @since 08/10/2019
 */
@Integration
@Slf4j
class ProxyEmailProviderServiceSpec extends MdmSpecification implements JsonComparer {

    EmailService emailService
    MessageSource messageSource
    ProxyEmailProviderService proxyEmailProviderService

    void 'Confirm service info'() {
        expect:
        proxyEmailProviderService.version == '2.0'
        proxyEmailProviderService.providerType == 'EmailProvider'
        proxyEmailProviderService.name == 'ProxyEmailProviderService'
        proxyEmailProviderService.displayName == 'Proxy Email Provider'
        proxyEmailProviderService.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy'
    }

    void 'Build basic message for special email service'() {
        given:
        proxyEmailProviderService.allProps.emailServiceUsername = 'svc_user'
        proxyEmailProviderService.allProps.emailServicePassword = 'svc_password'

        when:
        String msg = proxyEmailProviderService.buildMessage('MDM', 'mdm@mdm.com', ['test user': 'test@test.com'], [:], 'Test', 'Hello')

        then:
        verifyJson('''{
  "fromName": "MDM",
  "fromAddress": "mdm@mdm.com",
  "to": [{"test user": "test@test.com"}],
  "subject": "Test",
  "body": "Hello",
  "username": "svc_user",
  "password": "svc_password"
}''',
                   msg)
    }

    void 'Build multiple to and cc basic message'() {
        given:
        proxyEmailProviderService.allProps.emailServiceUsername = 'svc_user'
        proxyEmailProviderService.allProps.emailServicePassword = 'svc_password'

        when:
        String msg = proxyEmailProviderService.buildMessage('MDM', 'mdm@mdm.com',
                                                            ['test user': 'test@test.com', 'test user1': 'test1@test.com'],
                                                            ['cc user': 'cc@test.com', 'cc user1': 'cc1@test.com'], 'Test', 'Hello')

        then:
        verifyJson('''{
  "fromName": "MDM",
  "fromAddress": "mdm@mdm.com",
  "to": [{"test user": "test@test.com"},{"test user1": "test1@test.com"}],
  "cc": [{"cc user": "cc@test.com"},{"cc user1": "cc1@test.com"}],
  "subject": "Test",
  "body": "Hello",
  "username": "svc_user",
  "password": "svc_password"
}''',
                   msg)
    }

    void "Test sending email, will fail without credentials"() {
        given:
        SendEmailTask task = new SendEmailTask(emailService)
            .to('Ollie', 'ollie.freeman@gmail.com')
            .from('MDM', 'mdm@mdm.com')
            .subject('Test')
            .body('Hello')
        proxyEmailProviderService.allProps.emailServiceUrl = 'http://localhost'

        when:
        def res = proxyEmailProviderService.sendEmail(task)
        then:
        res != true
    }

}
