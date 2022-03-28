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
package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User

import asset.pipeline.grails.AssetResourceLocator
import grails.config.Config
import grails.core.GrailsApplication
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import java.sql.Driver

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootStrap {

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    GrailsApplication grailsApplication
    ApiPropertyService apiPropertyService
    SessionService sessionService
    AuthorityService authorityService
    EmailService emailService

    AsyncJobService asyncJobService

    AssetResourceLocator assetResourceLocator

    @Autowired
    MessageSource messageSource

    def init = {servletContext ->

        String grailsEnv = Environment.current.name
        String mdmEnv = System.getProperty('mdm.env')
        String jenkinsEnv = System.getenv('JENKINS')

        StringBuilder sb = new StringBuilder('Grails Environment: ')
        if (jenkinsEnv) sb.append('Jenkins ')
        sb.append(grailsEnv)
        log.debug('{}', sb.toString())
        if (mdmEnv) log.debug('MDM Environment: {}', mdmEnv)

        if (grailsApplication.config.getProperty('maurodatamapper.security.public', Boolean)) {
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

        sessionService.initialiseToContext()
        loadApiProperties(tmpDir)
        configureEmailProviderServices(grailsApplication.config)
        loadDefaultAuthority()

        log.debug('Main bootstrap complete')

        environments {
            development {
                Folder.withNewTransaction {
                    if (Folder.countByLabel('Development Folder') == 0) {
                        Folder folder = new Folder(label: 'Development Folder', createdBy: StandardEmailAddress.DEVELOPMENT)
                        checkAndSave(messageSource, folder)
                    }
                }
                Classifier.withNewTransaction {
                    if (Classifier.countByLabel('Development Classifier') == 0) {
                        Classifier classifier = new Classifier(label: 'Development Classifier', createdBy: StandardEmailAddress.DEVELOPMENT)
                        checkAndSave(messageSource, classifier)
                    }
                }
                log.debug('Development environment bootstrap complete')
            }
        }
    }

    def destroy = {
        asyncJobService.cancelAllRunningJobs()
        asyncJobService.shutdownAndAwaitTermination()
        emailService.shutdownAndAwaitTermination()
    }

    void loadApiProperties(String path) {

        User bootstrapUser = BootStrapUser.instance
        apiPropertyService.loadDefaultPropertiesIntoDatabase(bootstrapUser)
        apiPropertyService.loadLegacyPropertiesFromDefaultsFileIntoDatabase(path, bootstrapUser)

        // Override the email from address with either the config set variable or the simplejavamail username
        if (!apiPropertyService.findByKey(ApiPropertyEnum.EMAIL_FROM_ADDRESS.key)) {
            String fromEmailAddressToUse = grailsApplication.config.getProperty('maurodatamapper.email.from.address',
                                                                                String,
                                                                                grailsApplication.config.getProperty('simplejavamail.smtp.username',
                                                                                                                     String, ''))
            apiPropertyService.findAndUpdateByApiPropertyEnum(ApiPropertyEnum.EMAIL_FROM_ADDRESS,
                                                              fromEmailAddressToUse, bootstrapUser)
        }
        // Check for site url and set if provided by config
        // We do not override any site url which has already been set
        apiPropertyService.checkAndSetSiteUrl(grailsApplication.config.getProperty('grails.serverURL', String),
                                              grailsApplication.config.getProperty('grails.contextPath', String),
                                              bootstrapUser)
    }

    boolean configureEmailProviderServices(Config config) {
        log.info('Configuring emailers')
        Set<EmailProviderService> emailers = mauroDataMapperServiceProviderService.getEmailProviderServices()
        if (!emailers) {
            log.warn('No email plugins found, so the system will not be able to send emails')
            return false
        }

        if (emailers.size() > 1) {
            log.warn('Multiple email plugins found - we\'ll use the first one')
        }

        emailers.every { emailer ->
            log.debug('Configuring emailer: {}/{}', emailer.namespace, emailer.name)
            try {
                boolean configured = emailer.configure(config.toProperties())
                if (configured) emailer.testConnection()
                emailer.enabled = true
                true
            } catch (Exception e) {
                log.error("Cannot enable email plugin ${emailer.name}: ${e.message}")
                emailer.enabled = false
                false
            }
        }
    }

    void loadDefaultAuthority() {
        Authority.withNewTransaction {
            if (!authorityService.defaultAuthorityExists() && grailsApplication.config.getProperty('maurodatamapper.bootstrap.authority', Boolean)) {
                Authority authority = new Authority(label: grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY, String),
                                                    url: grailsApplication.config.getProperty(Authority.DEFAULT_URL_CONFIG_PROPERTY, String),
                                                    createdBy: StandardEmailAddress.ADMIN,
                                                    readableByEveryone: true,
                                                    readableByAuthenticatedUsers: true,
                                                    defaultAuthority: true)
                checkAndSave(messageSource, authority)
            }
        }
    }

    @Singleton
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
        Path getPath() {
            Path.from('cu', emailAddress)
        }

        @Override
        String getDomainType() {
            BootStrapUser
        }
    }
}
