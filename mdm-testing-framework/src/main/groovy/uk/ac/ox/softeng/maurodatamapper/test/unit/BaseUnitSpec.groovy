package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.plugin.json.view.mvc.JsonViewResolver
import grails.testing.gorm.DataTest
import grails.testing.spock.OnceBefore
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
        workingDirectory = Paths.get(config.'user.dir' as String)
        if (config.'grails.project.base.dir' as String) {
            Path projectDir = Paths.get(config.'grails.project.base.dir' as String)
            if (projectDir) {
                workingDirectory = (projectDir.fileName == workingDirectory.fileName) ? workingDirectory : workingDirectory.resolve(projectDir)
            }
        }
        workingDirectory
    }

    static Path getRootGrailsDirectory(def config) {
        workingDirectory = Paths.get(config.'user.dir' as String)
        if (config.'grails.root.base.dir' as String) {
            Path projectDir = Paths.get(config.'grails.root.base.dir' as String)
            if (projectDir) {
                workingDirectory = (projectDir.fileName == workingDirectory.fileName) ? workingDirectory : workingDirectory.resolve(projectDir)
            }
        }
        workingDirectory
    }

    def setup() {
        log.debug('Setting up base unit')
    }

    def cleanup() {
        log.debug('Cleaning up base unit')
    }

    @OnceBefore
    def loadI18nMessages() {
        loadI18nMessagesFromPath(getRootGrailsDirectory(config).resolve('mdm-core/grails-app/i18n/messages.properties'))
        loadI18nMessagesFromPath(getGrailsDirectory(config).resolve('grails-app/i18n/messages.properties'))

        try {
            // This makes sure the unit tests get the loaded messages
            // All the messageSource variables are autowired false and instantiated with a clean object
            // Don't know why the wiring isnt working but this solves it
            JsonViewResolver resolver = applicationContext.getBean(JsonViewResolver)
            resolver.templateEngine.messageSource = applicationContext.getBean(MessageSource)
        } catch (NoSuchBeanDefinitionException ignored) {}
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
        } else {
            log.warn('MessageSource does not extent StaticMessageSource')
        }
    }
}
