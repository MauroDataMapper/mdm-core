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
package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import grails.views.ResolvableGroovyTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j

/**
 * @since 16/11/2017
 */
@Slf4j
trait TemplateBasedExporter {

    abstract ResolvableGroovyTemplateEngine getTemplateEngine()

    String getExportViewPath() {
        '/exportModel/export'
    }

    ByteArrayOutputStream exportModel(ExportModel exportModel, String format) {
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('TBE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }

        def writable = template.make(exportModel: exportModel)
        def sw = new StringWriter()
        writable.writeTo(sw)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        os
    }
}
