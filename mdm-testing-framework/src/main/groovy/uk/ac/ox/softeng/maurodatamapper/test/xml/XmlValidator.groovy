package uk.ac.ox.softeng.maurodatamapper.test.xml

import grails.util.Holders
import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

import static org.junit.Assert.assertTrue

/**
 * @since 02/05/2018
 */
@Slf4j
trait XmlValidator extends XmlComparer {

    static String getDateTimeString() {
        getDateTimeString(OffsetDateTime.now(ZoneId.of('UTC')))
    }

    static String getDateTimeString(OffsetDateTime offsetDateTime) {
        offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    static String getDateString() {
        getDateString(LocalDate.now())
    }

    static String getDateString(LocalDate localDate) {
        localDate.format(DateTimeFormatter.ISO_DATE)
    }

    boolean validateXml(String xsdFileName, String xsdVersion, String xml) {

        def config = Holders.findApplication().config

        Path userDir = Paths.get(config."user.dir" as String)
        Path schemaFolderPath = userDir.resolve('grails-app/assets/xsd')

        Path schemaFilePath = schemaFolderPath.resolve("${xsdFileName}_${xsdVersion}.xsd")

        validateXml(schemaFilePath, xml)
    }

    boolean validateXml(Path schemaFilePath, String xml) {

        if (!Files.exists(schemaFilePath)) throw new IllegalArgumentException("Schema file ${schemaFilePath} does not exist")

        def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

        def schema = factory.newSchema(new StreamSource(schemaFilePath.toFile()))
        def validator = schema.newValidator()
        try {
            validator.validate(new StreamSource(new StringReader(xml)))
        } catch (Exception ex) {
            failureReason = ex.getMessage()
            try {
                log.error(XmlUtil.serialize(xml))
            } catch (Exception ignored) {
                log.error("$xml")
            }
            log.error('Invalid XML validation: {}', ex.getMessage())
            return false
        }
        true
    }

    void validateAndCompareXml(String expectedXml, String actualXml, String schemaName, String schemaVersion) {

        // Data verification in DB
        def xmlIsValid = validateXml(schemaName, schemaVersion, actualXml)
        assertTrue failureReason, xmlIsValid
        completeCompareXml(expectedXml, actualXml)
    }
}
