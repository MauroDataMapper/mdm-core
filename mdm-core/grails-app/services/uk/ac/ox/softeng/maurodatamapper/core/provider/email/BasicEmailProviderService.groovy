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

import uk.ac.ox.softeng.maurodatamapper.core.email.SendEmailTask

import groovy.util.logging.Slf4j
import org.simplejavamail.MailException
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.email.Recipient
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.config.ConfigLoader
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder

import javax.mail.Message

@Slf4j
class BasicEmailProviderService extends EmailProviderService {

    Map allProps
    Properties simpleMailProps


    @Override
    int getOrder() {
        LOWEST_PRECEDENCE
    }

    @Override
    String getVersion() {
        '2.0'
    }

    @Override
    String getDisplayName() {
        'Basic Email Provider'
    }

    @Override
    boolean configure(Map properties) {
        allProps = properties
        simpleMailProps = new Properties()
        for (ConfigLoader.Property p : ConfigLoader.Property.values()) {
            if (properties[p.key()]) {
                log.trace('Setting email property {}', p.key())
                simpleMailProps.setProperty(p.key(), properties[p.key()] as String)
            }
        }
        ConfigLoader.loadProperties(simpleMailProps, false)
        true
    }

    @Override
    void testConnection() {
        buildMailer().testConnection()
    }

    @Override
    String sendEmail(SendEmailTask sendEmailTask) {

        log.info('Sending email to {}', sendEmailTask.to)
        try {
            Email email = buildEmail(sendEmailTask)
            log.debug('Email built')
            buildMailer().sendMail(email)
            log.debug('Email sent successfully')
            return null
        } catch (MailException e) {
            String failureReason = extractFullFailureReason(e)
            log.error('Email sending failed: {}', failureReason)
            return failureReason
        } catch (IllegalArgumentException e) {
            log.error('Email sending failed: {}', e.message)
            return e.message
        }
    }

    @Override
    String validateEmail(SendEmailTask sendEmailTask) {
        log.info('Validating email to {}', sendEmailTask.to)
        try {
            Email email = buildEmail(sendEmailTask)
            buildMailer().validate(email)
            log.debug('Email validated successfully')
            return null
        } catch (MailException e) {
            String failureReason = extractFullFailureReason(e)
            log.error('Email validation failed: {}', failureReason)
            return failureReason
        } catch (IllegalArgumentException e) {
            log.error('Email validation failed: {}', e.message)
            return e.message
        }
    }

    Email buildEmail(SendEmailTask sendEmailTask) {
        EmailBuilder.startingBlank()
            .from(sendEmailTask.fromName, sendEmailTask.fromAddress)
            .withSubject(sendEmailTask.subject)
            .appendText(sendEmailTask.body)
            .to(sendEmailTask.to.collect {new Recipient(it.key, it.value, Message.RecipientType.TO)})
            .cc(sendEmailTask.cc.collect {new Recipient(it.key, it.value, Message.RecipientType.CC)})
            .buildEmail()
    }

    Mailer buildMailer() {
        Mailer mailer = MailerBuilder.buildMailer()
        log.debug('{}', mailer.getSession().getProperties())
        mailer
    }

    String extractFullFailureReason(Throwable throwable) {
        if (throwable.getCause()) return "${throwable.message.trim()}::${extractFullFailureReason(throwable.getCause())}"
        throwable.message.trim()
    }
}
