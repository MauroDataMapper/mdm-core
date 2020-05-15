package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.config.Config
import grails.core.GrailsApplication
import grails.util.Environment
import groovy.util.logging.Slf4j

import java.sql.Driver

@Slf4j
class BootStrap {

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    GrailsApplication grailsApplication
    ApiPropertyService apiPropertyService
    SessionService sessionService

    def init = {servletContext ->
        Utils.outputRuntimeArgs(BootStrap)
        log.debug('Grails Environment: {} (mdm.env property : {})', Environment.current.name, System.getProperty('mdm.env') ?: '')
        if (grailsApplication.config.maurodatamapper.security.public) {
            log.warn('Full public access is turned on')
        }

        // Ensure all sql drivers are registered
        // When running in tomcat the Drivermanager is initialised before anything else, so the jar files inside MDM are not scanned
        // This single line loads all the service defined Driver classes, they are registered as they are loaded.
        ServiceLoader.load(Driver).collect()

        String tmpDir = System.getProperty('java.io.tmpdir')
        tmpDir = tmpDir ? '/tmp' : tmpDir

        if (!tmpDir.endsWith('/')) tmpDir += '/'

        tmpDir += "${Environment.current.name}/"

        log.info('Deployment tmp dir: {}', tmpDir)


        sessionService.initialiseToContext(servletContext)
        loadApiProperties(tmpDir)
        configureEmailers(grailsApplication.config)
    }
    def destroy = {
    }

    void loadApiProperties(String path) {

        User bootstrapUser = new BootStrapUser()
        apiPropertyService.loadDefaultPropertiesIntoDatabase(bootstrapUser)
        apiPropertyService.loadLegacyPropertiesFromDefaultsFileIntoDatabase(path, bootstrapUser)

        // Override the email from address with whatever is set to actually send emails
        apiPropertyService.findAndUpdateByApiPropertyEnum(ApiPropertyEnum.EMAIL_FROM_ADDRESS,
                                                          grailsApplication?.config?.simplejavamail?.smtp?.username,
                                                          bootstrapUser)
    }

    boolean configureEmailers(Config config) {
        log.info('Configuring emailers')
        Set<EmailProviderService> emailers = mauroDataMapperServiceProviderService.getEmailProviderServices()
        if (!emailers) {
            log.warn('No email plugins found, so the system will not be able to send emails')
            return false
        }

        if (emailers.size() > 1) {
            log.warn('Multiple email plugins found - we\'ll use the first one')
        }

        emailers.every {emailer ->
            log.debug('Configuring emailer: {}/{}', emailer.namespace, emailer.name)
            try {
                return emailer.configure(config)
            } catch (Exception e) {
                log.error("Cannot configure email plugin ${emailer.name}", e)
                return false
            }
        }
    }

    static class BootStrapUser implements User {
        String firstName = 'Bootstrap'
        String lastName = 'User'
        String emailAddress = 'bootstrap.user@maurodatamapper.com'
        String tempPassword = ''

        @Override
        UUID getId() {
            UUID.randomUUID()
        }

        UUID ident() {
            id
        }

        @Override
        String getDomainType() {
            BootStrapUser
        }
    }
}
