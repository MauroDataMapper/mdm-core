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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer.XmlImportMapping
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.provider.importer.parameter.DataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import groovy.xml.slurpersupport.NodeChild
import org.springframework.core.io.Resource

import java.nio.charset.Charset
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

@Slf4j
class DataModelXmlImporterService extends DataBindDataModelImporterProviderService<DataModelFileImporterProviderServiceParameters> implements XmlImportMapping {

    AssetResourceLocator assetResourceLocator

    @Override
    String getDisplayName() {
        'XML DataModel Importer'
    }

    @Override
    String getVersion() {
        '5.1'
    }

    @Override
    Boolean canImportMultipleDomains() {
        true
    }

    @Override
    DataModel importDataModel(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing DataModel map')
        bindMapToDataModel(currentUser, backwardsCompatibleExtractDataModelMap(result, map))
    }

    @Override
    List<DataModel> importDataModels(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('XIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('XIS02', 'Cannot import empty content')

        String xml = new String(content, Charset.defaultCharset())

        log.debug('Parsing in file content using XmlSlurper')
        GPathResult result = new XmlSlurper().parseText(xml)
        result = result.children()[0].name() == 'dataModels' ? result.children()[0] : result

        if (result.name() == 'dataModels') {
            log.debug('Importing DataModel list')
            List<Map> xmlMaps = convertToList(result as NodeChild)
            List<Map> dataModelMaps = xmlMaps.findAll {it}
            if (!dataModelMaps) throw new ApiBadRequestException('XIS03', 'Cannot import XML as dataModel/s is not present')
            if (dataModelMaps.size() < xmlMaps.size()) log.warn('Cannot import certain XML as dataModel/s is not present')

            log.debug('Importing list of DataModel maps')
            return dataModelMaps.collect {bindMapToDataModel(currentUser, new HashMap(it))}
        }

        // Handle single DataModel map or exportModel passed to this method, for backwards compatibility

        log.debug('Converting result to Map')
        Map map = convertToMap(result)

        log.debug('Importing DataModel map')
        [bindMapToDataModel(currentUser, backwardsCompatibleExtractDataModelMap(result, map))]
    }

    List convertToList(NodeChild nodeChild) {
        nodeChild.children().collect {convertToMap(it)}
    }

    String validateXml(String xml) {
        Resource xsdResource = assetResourceLocator.findAssetForURI("dataModel_${version}.xsd")

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

    private Map backwardsCompatibleExtractDataModelMap(GPathResult result, Map map) {
        if (result.name() == 'exportModel' && map.dataModel && map.dataModel instanceof Map) return map.dataModel as Map
        if (result.name() == 'dataModel') return map
        throw new ApiBadRequestException('XIS03', 'Cannot import XML as dataModel is not present')
    }
}
