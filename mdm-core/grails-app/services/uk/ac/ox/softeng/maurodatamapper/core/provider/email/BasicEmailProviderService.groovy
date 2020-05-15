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
