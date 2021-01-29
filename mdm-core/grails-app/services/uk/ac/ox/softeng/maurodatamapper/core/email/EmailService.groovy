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
package uk.ac.ox.softeng.maurodatamapper.core.email


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.commons.text.StringSubstitutor

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
class EmailService {

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ApiPropertyService apiPropertyService

    private static EmailProviderService emailInstance

    List<Email> list(Map pagination = [:]) {
        Email.withFilter(pagination).list(pagination)
    }

    EmailProviderService getEmailer() {
        if (!emailInstance) {
            emailInstance = mauroDataMapperServiceProviderService.getEmailProvider()
        }
        emailInstance
    }

    @Transactional
    String sendEmail(String fromName, String fromAddress, Map<String, String> to, Map<String, String> cc, String subject, String messageBody) {
        if (emailer != null) {
            Email email = new Email(sentToEmailAddress: to.values().first(), subject: subject, body: messageBody,
                                    emailServiceUsed: emailer.getDisplayName(),
                                    dateTimeSent: OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                                    successfullySent: false).save(flush: true)

            log.info('Sending an email with emailer "{}"', emailer.getDisplayName())

            def emailResult = emailer.sendEmail(fromName, fromAddress, to, cc, subject, messageBody)

            if (emailResult instanceof String) {
                email.successfullySent = false
                email.failureReason = emailResult
            } else {
                email.successfullySent = true
            }

            email.save(flush: true)
            return email.failureReason
        }
        log.info('No emailer configured.  Would have sent message with content: {}', messageBody)
        'No emailer configured'
    }

    Map<String, String> buildUserPropertiesMap(User user, InformationAware informationAwareItem, String baseUrl, String passwordResetLink) {
        Map<String, String> ret = [
            catalogueUrl: baseUrl,
            firstName   : user.getFirstName(),
            lastName    : user.getLastName(),
            emailAddress: user.getEmailAddress(),
            tempPassword: user.getTempPassword()
        ]
        if (informationAwareItem) {
            ret.itemLabel = informationAwareItem.getLabel()
        }
        if (passwordResetLink) {
            ret.passwordResetLink = passwordResetLink
        }

        ret
    }

    void sendEmailToUser(String baseUrl, ApiPropertyEnum subjectProperty,
                         ApiPropertyEnum bodyProperty,
                         User user) {
        sendEmailToUser(baseUrl, subjectProperty, bodyProperty, user, null, null)
    }

    void sendEmailToUser(String baseUrl, ApiPropertyEnum subjectProperty,
                         ApiPropertyEnum bodyProperty, User user,
                         InformationAware informationAwareItem) {
        sendEmailToUser(baseUrl, subjectProperty, bodyProperty, user, informationAwareItem, null)
    }

    void sendEmailToUser(String baseUrl, ApiPropertyEnum subjectProperty,
                         ApiPropertyEnum bodyProperty,
                         User user,
                         String passwordResetLink) {
        sendEmailToUser(baseUrl, subjectProperty, bodyProperty, user, null, passwordResetLink)
    }

    void sendEmailToUser(String baseUrl, ApiPropertyEnum subjectProperty,
                         ApiPropertyEnum bodyProperty,
                         User user,
                         InformationAware informationAwareItem, String passwordResetLink) {

        Thread thread = new Thread(new Runnable() {

            @Override
            void run() {
                log.debug('Sending email')
                Map<String, String> to = [:]
                to["${user.getFirstName()} ${user.getLastName()}".toString()] = user.getEmailAddress()
                Map<String, String> propertiesMap = buildUserPropertiesMap(user, informationAwareItem, baseUrl, passwordResetLink)
                try {
                    String messageBody = getApiPropertyAndSubstitute(bodyProperty, propertiesMap)
                    String messageSubject = getApiPropertyAndSubstitute(subjectProperty, propertiesMap)
                    String fromName = getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_NAME, propertiesMap)
                    String fromAddress = getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_ADDRESS, propertiesMap)
                    String result = sendEmail(fromName, fromAddress, to, [:], messageSubject, messageBody)
                    if (result) log.warn('Email was not sent: [{}]', result)
                    else log.debug('Email sent successfully')
                } catch (ApiException apiException) {
                    log.error('Could not send email', apiException)
                } catch (IllegalStateException illegalStateException) {
                    // Only going to happen in the event the application shuts down before the email is saved
                    // However as we daemonise the process of sending the email this is possible.
                    log.warn("Possible failure to send email due to IllegalStateException: ${illegalStateException.message}")
                }
            }
        })

        thread.start()
    }

    String getApiPropertyAndSubstitute(ApiPropertyEnum key,
                                       Map<String, String> propertiesMap) {
        ApiProperty property = apiPropertyService.findByApiPropertyEnum(key)
        if (property) {
            StringSubstitutor sub = new StringSubstitutor(propertiesMap)
            return sub.replace(property.value)
        }
        null
    }
}
