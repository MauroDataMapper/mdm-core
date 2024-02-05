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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.email.SendEmailTask
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService

import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.text.Template
import org.springframework.beans.factory.annotation.Autowired

class ProxyEmailProviderService extends EmailProviderService {

    private static final String MESSAGE_VIEW_PATH = '/email/message'

    Map allProps

    @Autowired
    JsonViewTemplateEngine templateEngine

    ProxyEmailProviderService() {
        allProps = [:]
    }

    @Override
    boolean configure(Map properties) {
        allProps = properties
        true
    }

    @Override
    String sendEmail(SendEmailTask sendEmailTask) {

        if (!allProps.emailServiceUrl) {
            throw new ApiInternalException('MC-PEPS01',
                                           'Required property "emailServiceUrl" has not been supplied')
        }

        log.warn('Sending email via special email service!')

        String msg = buildMessage(sendEmailTask.fromName, sendEmailTask.fromAddress, sendEmailTask.to, sendEmailTask.cc, sendEmailTask.subject, sendEmailTask.body)
        log.warn(msg)

        URL baseUrl = new URL(allProps.emailServiceUrl as String)
        try {
            URLConnection connection = baseUrl.openConnection()
            String response = connection.with {
                doOutput = true
                requestMethod = 'POST'
                outputStream.withWriter {it << msg}
                it
            }
                .inputStream
                .withReader {it.text}
            log.warn(response)
            null
        } catch (Exception ex) {
            return extractFullFailureReason(ex)
        }
    }

    @Override
    void testConnection() {
        URL baseUrl = new URL(allProps.emailServiceUrl as String)
        baseUrl.openConnection()
    }

    @Override
    int getOrder() {
        0
    }

    @Override
    String getVersion() {
        '2.0'
    }

    @Override
    String getDisplayName() {
        'Proxy Email Provider'
    }

    String extractFullFailureReason(Throwable throwable) {
        if (throwable.getCause()) return "${throwable.message.trim()}::${extractFullFailureReason(throwable.getCause())}"
        throwable.message.trim()
    }

    String buildMessage(String fromName, String fromAddress, Map<String, String> to, Map<String, String> cc,
                        String subject, String body) {
        Template template = templateEngine.resolveTemplate(MESSAGE_VIEW_PATH)

        if (!template) {
            log.error('Could not find template at path {}', MESSAGE_VIEW_PATH)
            throw new ApiInternalException('MC-PEPS02',
                                           "Could not find template at path ${MESSAGE_VIEW_PATH}")
        }

        def writable = template.make(fromName: fromName,
                                     fromAddress: fromAddress,
                                     to: to,
                                     cc: cc,
                                     subject: subject,
                                     body: body,
                                     username: allProps.emailServiceUsername,
                                     password: allProps.emailServicePassword)
        def sw = new StringWriter()
        writable.writeTo(sw)
        sw.toString()
    }
}
