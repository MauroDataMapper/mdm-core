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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild
import groovy.xml.slurpersupport.NodeChildren
import org.springframework.core.io.Resource

import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

@Slf4j
class DataFlowXmlImporterService extends DataBindDataFlowImporterProviderService<DataFlowFileImporterProviderServiceParameters> {

    AssetResourceLocator assetResourceLocator

    @Override
    String getDisplayName() {
        'XML DataFlow Importer'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.toLowerCase() == 'application/mauro.dataflow+xml'
    }

    @Override
    DataFlow importDataFlow(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing DataFlow map')
        bindMapToDataFlow currentUser, backwardsCompatibleExtractDataFlowMap(result, map)
    }

    @Override
    List<DataFlow> importDataFlows(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        List<DataFlow> imported = []
        if (result.name() == 'dataFlows') {
            log.debug('Importing DataFlow list')
            List list = convertToList(result as NodeChild)
            list.each {
                imported += bindMapToDataFlow(currentUser, it as Map)
            }
        } else {
            // Handle single DM map or exportModel being passed to this method
            Map map = convertToMap(result)
            log.debug('Importing DataFlow map')
            imported += bindMapToDataFlow currentUser, backwardsCompatibleExtractDataFlowMap(result, map)
        }

        imported
    }

    Map backwardsCompatibleExtractDataFlowMap(GPathResult result, Map map) {
        switch (result.name()) {
            case 'exportModel':
                return map.dataFlow as Map
            case 'dataFlow':
                return map
        }
        throw new ApiBadRequestException('XIS03', 'Cannot import XML as dataFlow is not present')
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

    String validateXml(String xml) {

        Resource xsdResource = assetResourceLocator.findAssetForURI("dataFlow_${version}.xsd")

        def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        def schema = factory.newSchema(new StreamSource(xsdResource.inputStream))
        def validator = schema.newValidator()
        try {
            validator.validate(new StreamSource(new StringReader(xml)))
        } catch (Exception ex) {
            return ex.getMessage()
        }
        null
    }
}
