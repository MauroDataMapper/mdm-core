/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.rest.render

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty

import grails.rest.render.AbstractIncludeExcludeRenderer
import grails.rest.render.RenderContext
import grails.web.mime.MimeType
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.grails.datastore.mapping.model.MappingContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class MdmCsvApiPropertyRenderer<T> extends AbstractIncludeExcludeRenderer<T> {

    @Autowired
    @Qualifier('grailsDomainClassMappingContext')
    MappingContext mappingContext

    MdmCsvApiPropertyRenderer(Class<T> targetType) {
        super(targetType, new MimeType('text/csv', 'csv'))
    }

    @Override
    void render(T object, RenderContext context) {
        context.contentType = 'text/csv'
        final entity = mappingContext.getPersistentEntity(object.class.name)
        boolean isDomain = entity != null

        CSVPrinter printer = new CSVPrinter(context.writer, CSVFormat.DEFAULT)

        printHeader(printer)

        if (isDomain) {
            printApiProperty(printer, object)
        } else if (object instanceof Collection) {
            for (ApiProperty o in ((Collection) object)) {
                printApiProperty(printer, o)
            }
        }

        printer.flush()
        printer.close()
        context.writer.flush()
    }

    private void printHeader(CSVPrinter printer) {
        printer.printRecord(['id', 'key', 'value', 'category', 'publiclyVisible', 'lastUpdatedBy', 'createdBy', 'lastUpdated'])
    }

    private void printApiProperty(CSVPrinter printer, ApiProperty o) {
        printer.printRecord([o.id, o.key, o.value, o.category, o.publiclyVisible, o.lastUpdatedBy, o.createdBy, o.lastUpdated])
    }
}
