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
package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.plugin.json.view.mvc.JsonViewResolver
import grails.testing.gorm.DataTest
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import org.grails.testing.GrailsUnitTest
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.MessageSource
import org.springframework.context.support.StaticMessageSource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Due to the way static instances work in domain spaces and spock tests when using users in 'where' datasets
 * you will need to perform a get on the domain in the given section of the test to make sure the IDs are correct
 *
 * given:
 * def user = CatalogueUser.findByEmailAddress(worker?.emailAddress)
 * controller.catalogueUserService = Mock(CatalogueUserService) {* get(user?.id) >> user
 *}*/
@Slf4j
abstract class BaseUnitSpec extends MdmSpecification implements DataTest, GrailsUnitTest {

    static Path workingDirectory

    static Path getGrailsDirectory(def config) {
        workingDirectory = Paths.get(config.getProperty('user.dir', String))
        if (config.getProperty('grails.project.base.dir', String)) {
            Path projectDir = Paths.get(config.getProperty('grails.project.base.dir', String))
            if (projectDir) {
                workingDirectory = (projectDir.fileName == workingDirectory.fileName) ? workingDirectory : workingDirectory.resolve(projectDir)
            }
        }
        workingDirectory
    }

    static Path getRootGrailsDirectory(def config) {
        workingDirectory = Paths.get(config.getProperty('user.dir', String))
        if (config.getProperty('grails.root.base.dir', String)) {
            Path projectDir = Paths.get(config.getProperty('grails.root.base.dir', String))
            if (projectDir) {
                workingDirectory = (projectDir.fileName == workingDirectory.fileName) ? workingDirectory : workingDirectory.resolve(projectDir)
            }
        }
        workingDirectory
    }

    def cleanup() {
        log.debug('Cleaning up base unit')
    }

    @RunOnce
    def setup() {
        log.debug('Run Once setting up base unit')
        log.debug('Load I18n Messages')
        loadI18nMessagesFromPath(getRootGrailsDirectory(config).resolve('mdm-core/grails-app/i18n/messages.properties'))
        loadI18nMessagesFromPath(getGrailsDirectory(config).resolve('grails-app/i18n/messages.properties'))

        try {
            // This makes sure the unit tests get the loaded messages
            // All the messageSource variables are autowired false and instantiated with a clean object
            // Don't know why the wiring isnt working but this solves it
            JsonViewResolver resolver = applicationContext.getBean(JsonViewResolver)
            resolver.templateEngine.messageSource = applicationContext.getBean(MessageSource)
        } catch (NoSuchBeanDefinitionException ignored) {
            log.warn 'Failed to load messages into json views'
        }
    }

    def loadI18nMessagesFromPath(Path messagesFilePath) {
        if (!Files.exists(messagesFilePath)) {
            log.info('No messages file available at {}', messagesFilePath)
            return
        }
        if (messageSource instanceof StaticMessageSource) {
            Properties messages = new Properties()
            messages.load(new FileReader(messagesFilePath.toFile()))
            messages.stringPropertyNames().each {k ->
                (messageSource as StaticMessageSource).addMessage(k, Locale.default, messages.getProperty(k))
            }
            log.info('{} messages loaded from {}', messages.size(), messagesFilePath.toString())
        } else {
            log.warn('MessageSource does not extent StaticMessageSource')
        }
    }
}
