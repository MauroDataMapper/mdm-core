/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

//import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.NodeChild
import groovy.util.slurpersupport.NodeChildren
import org.springframework.core.io.Resource

import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

@Slf4j
class XmlImporterService extends DataBindTerminologyImporterProviderService<TerminologyFileImporterProviderServiceParameters> {

    //AssetResourceLocator assetResourceLocator

    @Override
    String getDisplayName() {
        'XML Terminology Importer'
    }

    @Override
    String getVersion() {
        '3.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    Terminology importTerminology(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XTIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XTIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing Terminology map')
        bindMapToTerminology currentUser, backwardsCompatibleExtractTerminologyMap(result, map)
    }

    Map backwardsCompatibleExtractTerminologyMap(GPathResult result, Map map) {
        log.debug("backwardsCompatibleExtractTerminologyMap")
        switch (result.name()) {
            case 'exportModel':
                return map.terminology as Map
            case 'terminology':
                return map
        }
        throw new ApiBadRequestException('XIS03', 'Cannot import XML as terminology is not present')
    }

    Map<String, Object> convertToMap(NodeChild nodes) {
        Map<String, Object> map = [:]
        if (nodes.children().isEmpty()) {
            map[nodes.name()] = nodes.text()
        } else {
            map = ((NodeChildren) nodes.children()).collectEntries {NodeChild child ->
                String name = child.name()
                def content = name == 'id' ? null : child.text()

                if (child.childNodes()) {
                    Collection<String> childrenNames = child.children().list().collect {it.name().toLowerCase()}.toSet()

                    if (childrenNames.size() == 1 && child.name().toLowerCase().contains(childrenNames[0])) content = convertToList(child)
                    else content = convertToMap(child)

                }

                [name, content]
            }
        }
        map
    }

    List convertToList(NodeChild nodeChild) {
        nodeChild.children().collect {convertToMap(it)}
    }

    /*String validateXml(String xml) {

        Resource xsdResource = assetResourceLocator.findAssetForURI("terminology_${version}.xsd")

        def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        def schema = factory.newSchema(new StreamSource(xsdResource.inputStream))
        def validator = schema.newValidator()
        try {
            validator.validate(new StreamSource(new StringReader(xml)))
        } catch (Exception ex) {
            return ex.getMessage()
        }
        null
    }*/
}
