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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.nio.charset.Charset

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.CSVRecord

@Slf4j
class CsvImporterService extends DataBindReferenceDataModelImporterProviderService<ReferenceDataModelFileImporterProviderServiceParameters> {

    @Override
    String getDisplayName() {
        'CSV Reference Data Importer'
    }

    @Override
    String getVersion() {
        '3.0'
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        false
    }

    @Override
    ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content) {
        if (!currentUser) throw new ApiUnauthorizedException('JIS01', 'User must be logged in to import model')
        if (content.size() == 0) throw new ApiBadRequestException('JIS02', 'Cannot import empty content')

        CSVFormat csvFormat = CSVFormat.newFormat((char)',').withHeader();

        CSVParser parser = csvFormat.parse(
        new InputStreamReader(new ByteArrayInputStream(content), "UTF8"));

        List headers = parser.getHeaderNames();
        //TODO add a referenceDataElement for each column heading

        for (CSVRecord record : parser) {
            try {
                headers.each {
                    //TODO create a new referenceDataValue for each value
                    log.debug(record.get(it));
                }
                
            } catch (Exception e) {
            throw new RuntimeException("Error at line "
            + parser.getCurrentLineNumber(), e);
        }
}
parser.close();
//printer.close();

        //log.debug('Parsing in file content using JsonSlurper')
        //def result = new JsonSlurper().parseText(new String(content, Charset.defaultCharset()))
        //Map terminology = result.terminology
        //if (!terminology) throw new ApiBadRequestException('JIS03', 'Cannot import JSON as terminology is not present')

        //log.debug('Importing Terminology map')
        //bindMapToTerminology currentUser, new HashMap(terminology)
    }
}
