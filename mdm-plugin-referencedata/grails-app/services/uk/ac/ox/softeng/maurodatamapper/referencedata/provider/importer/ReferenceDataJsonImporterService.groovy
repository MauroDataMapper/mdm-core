/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.exporter.ReferenceDataJsonExporterService
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.nio.charset.Charset

@Slf4j
class ReferenceDataJsonImporterService
    extends DataBindReferenceDataModelImporterProviderService<ReferenceDataModelFileImporterProviderServiceParameters> {

    @Override
    String getDisplayName() {
        'JSON Reference Data Importer'
    }

    @Override
    String getVersion() {
        '4.0'
    }

    @Override
    Boolean handlesContentType(String contentType) {
        contentType.equalsIgnoreCase(ReferenceDataJsonExporterService.CONTENT_TYPE)
    }

    @Override
    ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        log.debug('Parsing in file content using JsonSlurper')
        def result = new JsonSlurper().parseText(new String(content, Charset.defaultCharset()))
        Map referenceDataModel = result.referenceDataModel
        if (!referenceDataModel) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as referenceDataModel is not present')

        log.debug('Importing ReferenceDataModel map')
        bindMapToReferenceDataModel currentUser, new HashMap(referenceDataModel)
    }
}
