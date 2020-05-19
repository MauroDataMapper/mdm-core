package uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService

import grails.plugin.json.view.JsonViewTemplateEngine
import groovy.text.Template
import org.springframework.beans.factory.annotation.Autowired

class ProxyEmailProviderService implements EmailProviderService {

    private static final String MESSAGE_VIEW_PATH = '/email/message'

    Map allProps

    @Autowired
    JsonViewTemplateEngine templateEngine

    @Override
    boolean configure(Map properties) {
        allProps = properties
        true
    }

    @Override
    def sendEmail(String fromName, String fromAddress, Map<String, String> to, Map<String, String> cc,
                  String subject, String body) {

        if (!allProps.emailServiceUrl) {
            throw new ApiInternalException('MC-PEPS01',
                                           'Required property "emailServiceUrl" has not been supplied')
        }

        log.warn('Sending email via special email service!')

        String msg = buildMessage(fromName, fromAddress, to, cc, subject, body)
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
            true
        } catch (Exception ex) {
            return extractFullFailureReason(ex)
        }
    }

    @Override
    int getOrder() {
        0
    }

    @Override
    String getVersion() {
        '1.0'
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
