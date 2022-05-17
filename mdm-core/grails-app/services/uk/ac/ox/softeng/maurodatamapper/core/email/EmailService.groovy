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
package uk.ac.ox.softeng.maurodatamapper.core.email

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.commons.text.StringSubstitutor

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Slf4j
@SuppressFBWarnings('LI_LAZY_INIT_STATIC')
class EmailService implements AnonymisableService {

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ApiPropertyService apiPropertyService

    ExecutorService executorService

    EmailService() {
        executorService = Executors.newFixedThreadPool(5)
    }

    List<Email> list(Map pagination = [:]) {
        Email.withFilter(pagination).list(pagination)
    }

    EmailProviderService getEmailProviderService() {
        mauroDataMapperServiceProviderService.getEmailProvider()
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

        Map<String, String> propertiesMap = buildUserPropertiesMap(user, informationAwareItem, baseUrl, passwordResetLink)

        SendEmailTask task = new SendEmailTask(this)
            .using(getEmailProviderService())
            .from(getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_NAME, propertiesMap), getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_ADDRESS, propertiesMap))
            .to("${user.getFirstName()} ${user.getLastName()}", user.getEmailAddress())
            .subject(getApiPropertyAndSubstitute(subjectProperty, propertiesMap))
            .body(getApiPropertyAndSubstitute(bodyProperty, propertiesMap))

        executorService.submit(task)
    }

    /**
     * Alternative to the above method for use when the API Property keys are not in the ApiPropertyEnum
     * @param baseUrl
     * @param subjectProperty
     * @param bodyProperty
     * @param user
     * @param informationAwareItem
     */
    void sendEmailToUser(String baseUrl,
                         String subjectProperty,
                         String bodyProperty,
                         User user,
                         InformationAware informationAwareItem) {

        Map<String, String> propertiesMap = buildUserPropertiesMap(user, informationAwareItem, baseUrl, null)

        SendEmailTask task = new SendEmailTask(this)
            .using(getEmailProviderService())
            .from(getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_NAME, propertiesMap), getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_ADDRESS, propertiesMap))
            .to("${user.getFirstName()} ${user.getLastName()}", user.getEmailAddress())
            .subject(getApiPropertyAndSubstitute(subjectProperty, propertiesMap))
            .body(getApiPropertyAndSubstitute(bodyProperty, propertiesMap))

        executorService.submit(task)
    }

    /**
     * Alternative to when the properties map needs top be specified externally
     * @param subjectProperty
     * @param bodyProperty
     * @param user
     * @param Map<string, string> propertieMap
     */
    void sendEmailToUser(String subjectProperty,
                         String bodyProperty,
                         User user,
                         Map<String, String> propertiesMap) {

        SendEmailTask task = new SendEmailTask(this)
            .using(getEmailProviderService())
            .from(getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_NAME, propertiesMap), getApiPropertyAndSubstitute(ApiPropertyEnum.EMAIL_FROM_ADDRESS, propertiesMap))
            .to("${user.getFirstName()} ${user.getLastName()}", user.getEmailAddress())
            .subject(getApiPropertyAndSubstitute(subjectProperty, propertiesMap))
            .body(getApiPropertyAndSubstitute(bodyProperty, propertiesMap))

        executorService.submit(task)
    }

    /**
     * Delete all emails sent to the specified address
     * @param sentTo
     */
    void anonymise(String sentTo) {
        Email.findAllBySentToEmailAddress(sentTo).each { email ->
            email.delete()
        }
    }

    @Transactional
    void sendEmail(SendEmailTask sendEmailTask) {

        Email email = sendEmailTask.asEmail().save(flush: true)

        if (sendEmailTask.hasEmailProviderService()) {

            if (sendEmailTask.isValid()) {

                log.info('Sending an email with email provider service "{}"', emailProviderService.getDisplayName())

                sendEmailTask.result = emailProviderService.sendEmail(sendEmailTask)

                log.debug('Email sent with response [{}]', sendEmailTask.result)
            }
        } else {
            sendEmailTask.result = 'No email provider service configured'
        }

        email.failureReason = sendEmailTask.result
        email.successfullySent = sendEmailTask.wasSuccessfullySent()
        email.save(flush: true)
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

    String getApiPropertyAndSubstitute(String key,
                                       Map<String, String> propertiesMap) {
        ApiProperty property = apiPropertyService.findByKey(key)
        if (property) {
            StringSubstitutor sub = new StringSubstitutor(propertiesMap)
            return sub.replace(property.value)
        }
        null
    }

    void shutdownAndAwaitTermination() {
        Utils.shutdownAndAwaitTermination(executorService)
    }
}
