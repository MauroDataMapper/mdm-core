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
package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.session.SessionService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

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

    AssetResourceLocator assetResourceLocator

    @Autowired
    MessageSource messageSource

    def init = { servletContext ->
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
        loadDefaultAuthority()

        log.info("Using lucene index directory of: {}", grailsApplication.config.hibernate.search.default.indexBase)
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
    }

    void loadApiProperties(String path) {

        User bootstrapUser = BootStrapUser.instance
        apiPropertyService.loadDefaultPropertiesIntoDatabase(bootstrapUser)
        apiPropertyService.loadLegacyPropertiesFromDefaultsFileIntoDatabase(path, bootstrapUser)

        // Override the email from address with whatever is set to actually send emails
        apiPropertyService.findAndUpdateByApiPropertyEnum(ApiPropertyEnum.EMAIL_FROM_ADDRESS,
                                                          grailsApplication?.config?.simplejavamail?.smtp?.username,
                                                          bootstrapUser)
        // Check for site url and set if provided by config
        // We do not override any site url which has already been set
        apiPropertyService.checkAndSetSiteUrl(grailsApplication?.config?.grails?.serverURL,
                                              grailsApplication?.config?.grails?.contextPath,
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

        emailers.every { emailer ->
            log.debug('Configuring emailer: {}/{}', emailer.namespace, emailer.name)
            try {
                return emailer.configure(config)
            } catch (Exception e) {
                log.error("Cannot configure email plugin ${emailer.name}", e)
                return false
            }
        }
    }

    void loadDefaultAuthority() {
        Authority.withNewTransaction {
            if (!authorityService.defaultAuthorityExists()) {
                Authority authority = new Authority(label: grailsApplication.config.getProperty(Authority.DEFAULT_NAME_CONFIG_PROPERTY),
                                                    url: grailsApplication.config.getProperty(Authority.DEFAULT_URL_CONFIG_PROPERTY),
                                                    createdBy: StandardEmailAddress.ADMIN,
                                                    readableByEveryone: true)
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
        String getDomainType() {
            BootStrapUser
        }
    }

}
