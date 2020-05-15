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
    boolean sendEmail(String fromName, String fromAddress, Map<String, String> to, Map<String, String> cc, String subject, String messageBody) {
        if (emailer != null) {
            Email email = new Email(sentToEmailAddress: to.values().first(), subject: subject, body: messageBody,
                                    emailServiceUsed: emailer.getDisplayName(),
                                    dateTimeSent: OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                                    successfullySent: false).save()

            log.info('Sending an email with emailer "{}"', emailer.getDisplayName())

            def emailResult = emailer.sendEmail(fromName, fromAddress, to, cc, subject, messageBody)

            if (emailResult instanceof String) {
                email.successfullySent = false
                email.failureReason = emailResult
            } else {
                email.successfullySent = true
            }

            email.save()
            email.successfullySent
        }
        log.info('No emailer configured.  Would have sent message with content: {}', messageBody)
        false
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
                    if (sendEmail(fromName, fromAddress, to, [:], messageSubject, messageBody)) log.debug('Email sent successfully')
                    else log.warn('Email was not sent. Reason unknown')
                } catch (ApiException apiException) {
                    log.error('Could not send email!', apiException)
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
