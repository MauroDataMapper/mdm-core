/*
 * Copyright 2020 University of Oxford
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

import org.simplejavamail.MailException
import org.simplejavamail.email.Email
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.email.Recipient
import org.simplejavamail.mailer.Mailer
import org.simplejavamail.mailer.MailerBuilder
import org.simplejavamail.util.ConfigLoader

import javax.mail.Message

class BasicEmailProviderService implements EmailProviderService {

    Map allProps
    Properties simpleMailProps

    @Override
    boolean configure(Map properties) {
        allProps = properties
        simpleMailProps = new Properties()
        for (ConfigLoader.Property p : ConfigLoader.Property.values()) {
            if (properties[p.key()]) {
                simpleMailProps.setProperty(p.key(), properties[p.key()] as String)
            }
        }
        ConfigLoader.loadProperties(simpleMailProps, false)
        true
    }

    @Override
    def sendEmail(String fromName, String fromAddress, Map<String, String> to, Map<String, String> cc,
                  String subject, String body) {

        log.info('Sending email to {}', to)
        Email email = EmailBuilder.startingBlank()
            .from(fromName, fromAddress)
            .withSubject(subject)
            .appendText(body)
            .to(to.collect {new Recipient(it.key, it.value, Message.RecipientType.TO)})
            .cc(cc.collect {new Recipient(it.key, it.value, Message.RecipientType.CC)})
            .buildEmail()

        try {
            Mailer m = MailerBuilder.buildMailer()
            log.debug('{}', m.getSession().getProperties())
            m.sendMail(email)
            log.debug('Email sent successfully!')
            return true
        } catch (MailException e) {
            String failureReason = extractFullFailureReason(e)
            log.error('Email sending failed: {}', failureReason)
            return failureReason
        }
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE
    }

    @Override
    String getVersion() {
        '1.0'
    }

    @Override
    String getDisplayName() {
        'Basic Email Provider'
    }

    String extractFullFailureReason(Throwable throwable) {
        if (throwable.getCause()) return "${throwable.message.trim()}::${extractFullFailureReason(throwable.getCause())}"
        throwable.message.trim()
    }
}
