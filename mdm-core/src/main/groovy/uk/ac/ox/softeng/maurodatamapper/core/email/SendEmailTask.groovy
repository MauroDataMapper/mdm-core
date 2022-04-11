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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService

import groovy.util.logging.Slf4j

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * @since 08/10/2021
 */
@Slf4j
class SendEmailTask implements Runnable {

    Map<String, String> to = [:]
    Map<String, String> cc = [:]
    String body
    String subject
    String fromName
    String fromAddress
    EmailProviderService emailProviderService
    EmailService emailService
    String result

    SendEmailTask(EmailService emailService) {
        this.emailService = emailService
    }

    SendEmailTask to(String fullName, String emailAddress) {
        to[fullName] = emailAddress
        this
    }

    SendEmailTask body(String body) {
        this.body = body
        this
    }

    SendEmailTask subject(String subject) {
        this.subject = subject
        this
    }

    SendEmailTask from(String fullName, String emailAddress) {
        fromName = fullName
        fromAddress = emailAddress
        this
    }

    SendEmailTask using(EmailProviderService emailProviderService) {
        this.emailProviderService = emailProviderService
        this
    }

    boolean hasEmailProviderService() {
        emailProviderService
    }

    boolean isValid() {
        result = emailProviderService.validateEmail(this)
        !result
    }

    boolean wasSuccessfullySent() {
        !result
    }

    Email asEmail() {
        new Email(sentToEmailAddress: to.values().first(), subject: subject, body: body,
                  emailServiceUsed: emailProviderService?.displayName,
                  dateTimeSent: emailProviderService ? OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC) : null,
                  successfullySent: false)
    }

    @Override
    void run() {
        try {
            log.debug('Sending email')
            emailService.sendEmail(this)
            if (result) log.warn('Email was not sent: [{}]', result)
            else log.debug('Email sent successfully')
        } catch (ApiException apiException) {
            log.error('Could not send email', apiException)
        } catch (IllegalStateException illegalStateException) {
            // Only going to happen in the event the application shuts down before the email is saved
            // However as we daemonise the process of sending the email this is possible.
            log.warn("Possible failure to send email due to IllegalStateException: ${illegalStateException.message}")
        }
        catch (RuntimeException runtimeException) {
            log.error('Unhandled runtime exception', runtimeException)
            throw new ApiInternalException('ES01', 'Something went wrong sending email', runtimeException)
        }
        catch (Exception exception) {
            log.error('Unhandled exception', exception)
            throw new ApiInternalException('ES02', 'Something went wrong sending email', exception)
        }
    }
}
