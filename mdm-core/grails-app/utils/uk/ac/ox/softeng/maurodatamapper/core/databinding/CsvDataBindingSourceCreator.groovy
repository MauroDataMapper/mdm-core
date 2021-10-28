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
package uk.ac.ox.softeng.maurodatamapper.core.databinding

import org.grails.web.databinding.bindingsource.AbstractRequestBodyDataBindingSourceCreator
import org.grails.web.databinding.bindingsource.InvalidRequestBodyException
import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.databinding.SimpleMapDataBindingSource
import grails.web.mime.MimeType

import groovy.transform.CompileStatic

import javax.servlet.http.HttpServletRequest

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.http.HttpMethod

/**
 * Read CSV from the body of a request and convert to a Map, so that this Map can
 * be bound to an instance of a domain. Only the header and next line of the CSV are used.
 */
@CompileStatic
class CsvDataBindingSourceCreator extends AbstractRequestBodyDataBindingSourceCreator {

    @Override
    MimeType[] getMimeTypes() {
        [new MimeType('text/csv', 'csv'), new MimeType('application/csv', 'csv')] as MimeType[]
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, Object bindingSource) {
        if(bindingSource instanceof HttpServletRequest) {
            def req = (HttpServletRequest)bindingSource
            HttpMethod method = HttpMethod.resolve(req.method)
            if (req.contentLength != 0 && !ignoredRequestBodyMethods.contains(method)) {
                def bindingTargetInstance = bindingTargetType.getDeclaredConstructor().newInstance()
                if (bindingTargetInstance instanceof MdmDataBindingCollection) {
                    return new SimpleMapDataBindingSource(createCsvCollectionMap(req.getInputStream(), req.getCharacterEncoding()))
                } else {
                    return new SimpleMapDataBindingSource(createCsvMap(req.getInputStream(), req.getCharacterEncoding()))
                }

            }
        }

        // if the above didn't work then return binding for an empty map
        return new SimpleMapDataBindingSource([:])
    }

    @Override
    protected DataBindingSource createBindingSource(Reader reader) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected CollectionDataBindingSource createCollectionBindingSource(Reader reader) {
        throw new UnsupportedOperationException()
    }

    /**
     * Parse an InputStream and for a CSV like
     * header1,header2
     * value1,value2
     * return a map like [header1: value1, header2: value2]
     * @param inputStream
     * @param characterEncoding
     * @return A map like [header1: value1, header2: value2]
     */
    protected static Map createCsvMap(InputStream inputStream, String characterEncoding) {
        Map result = [:]
        CSVFormat csvFormat = CSVFormat.newFormat((char) ',')
                .withQuote((char) '"')
                .withHeader()

        CSVParser parser = csvFormat.parse(new InputStreamReader(inputStream, characterEncoding))

        List headers = parser.getHeaderNames()

        // If there is a first record then get it and make a map.
        // Any subsequent records are ignored.
        try {
            if (parser.iterator().hasNext()) {
                CSVRecord record = parser.iterator().next()

                headers.each {
                    result[it] = record.get(it)
                }
            }
        } catch (Exception e) {
            throw new InvalidRequestBodyException(e)
        }

        parser.close()
        result
    }

   /**
    * Parse an InputStream and for a CSV like
    * header1,header2
    * row1value1,row1value2
    * row2value1,row2value2
    * return a map like [count: 2, items: [[header1: row1value1, header2: row1value2], [header1: row2value1, header2: row2value2]]]
    * @param inputStream
    * @param characterEncoding
    * @return A map like [count: 2, items: [[header1: row1value1, header2: row1value2], [header1: row2value1, header2: row2value2]]]
    */
    protected static Map createCsvCollectionMap(InputStream inputStream, String characterEncoding) {
        Map result = [:]
        Collection items = []
        CSVFormat csvFormat = CSVFormat.newFormat((char) ',')
                .withQuote((char) '"')
                .withHeader()

        CSVParser parser = csvFormat.parse(new InputStreamReader(inputStream, characterEncoding))

        List headers = parser.getHeaderNames()

        try {
            while (parser.iterator().hasNext()) {
                CSVRecord record = parser.iterator().next()

                Map instance = [:]
                headers.each {header ->
                    instance[header] = record.get(header)
                }

                items.add(instance)
            }
        } catch (Exception e) {
            throw new InvalidRequestBodyException(e)
        }

        result["count"] = items.size()
        result["items"] = items

        parser.close()
        result
    }
}
