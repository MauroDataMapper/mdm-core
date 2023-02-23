/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

        Path userDir = Paths.get(config.getProperty('user.dir', String))
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
