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
package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImportParameterGroup
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.validation.ValidationErrors
import grails.web.databinding.DataBinder
import groovy.util.logging.Slf4j
import org.apache.commons.beanutils.PropertyUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import java.lang.reflect.Field
import javax.servlet.http.HttpServletRequest

@Slf4j
@SuppressWarnings('GrUnnecessaryPublicModifier')
class ImporterService implements DataBinder {

    // ClassifierService classifierService

    public <M extends Model, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> List<M> importModels(
        User currentUser, T importer, P importParams) {
        importer.importDomains(currentUser, importParams).findAll()
    }

    public <M extends Model, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> M importModel(
        User currentUser, T importer, P importParams) {
        M model = importer.importDomain(currentUser, importParams)

        if (!model) throw new ApiBadRequestException('IS01', "Failed to import Model using ${importer.name} importer")
        model
    }

    List<ImportParameterGroup> describeImporterParams(ImporterProviderService importer) {
        /* The following steps reduce the fields of the parameter class
         * * Finds all fields which have real names
         * * Group identical named fields
         * * Find fields which are implemented in the parameter class
         * * Flatten out all other fields
         * These steps allow us to have only real fields and to override field configurations in extending classes
         */
        List<Field> fields = FieldUtils.getAllFields(importer.importerProviderServiceParametersClass)
            .findAll {it.name.matches(/^[^_$].*/)}
            .groupBy {it.name}
            .collectMany {it.value.findAll {it.getDeclaringClass() == importer.importerProviderServiceParametersClass} ?: it.value}

        Map<String, ImportParameterGroup> groups = [:]

        fields.findAll {it.getAnnotation(ImportParameterConfig)}.each {field ->

            ImportParameterConfig config = field.getAnnotation(ImportParameterConfig)

            String fieldType

            switch (field.getType().getSimpleName()) {
                case 'FileParameter':
                    fieldType = 'File'
                    break
                case 'UUID':
                    fieldType = 'Folder'
                    break
                default:
                    fieldType = field.getType().getSimpleName()
            }

            if (config.password()) fieldType = 'Password'

            ImportParameterGroup group = groups.getOrDefault(config.group().name(), new ImportParameterGroup(name: config.group().name(),
                                                                                                             order: config.group().order()))

            group.addToImportParameters(
                name: field.name,
                type: fieldType,
                order: config.order(),
                optional: config.optional(),
                displayName: config.displayName(),
                description: config.description(),
                )
            groups[group.name] = group
        }

        groups.values().sort()
    }

    public <T extends ImporterProviderServiceParameters> Errors validateParameters(T paramsObj, Class<T> clazz) {
        Field[] fields = FieldUtils.getAllFields(clazz)
        Errors errors = new ValidationErrors(paramsObj, clazz.getName())
        for (Field field : fields) {
            ImportParameterConfig config = field.getAnnotation(ImportParameterConfig)
            if (config && !config.optional()) {
                Object o = PropertyUtils.getProperty(paramsObj, field.getName())
                if (!o?.toString()) {
                    errors.rejectValue(field.name, 'default.null.message',
                                       [field.name, clazz].toArray(), 'Property [{0}] of class [{1}] cannot be null')
                }
            }
        }
        errors
    }

    public <T extends ImporterProviderServiceParameters> T createNewImporterProviderServiceParameters(ImporterProviderService importer) {
        importer.createNewImporterProviderServiceParameters()
    }

    public <T extends ImporterProviderServiceParameters> T extractImporterProviderServiceParameters(ImporterProviderService ImporterProviderService,
                                                                                                    AbstractMultipartHttpServletRequest request) {
        Map importParamsMap = new HashMap<>(request.parameterMap)

        MultipartFile f = request.getFile('importFile')
        if (f && !f.isEmpty()) {
            importParamsMap[f.name] = new FileParameter(f.originalFilename, f.contentType, f.bytes)
        }

        T importerProviderServiceParameters = createNewImporterProviderServiceParameters(ImporterProviderService)
        bindData(importerProviderServiceParameters, importParamsMap)
        importerProviderServiceParameters
    }

    public <T extends ImporterProviderServiceParameters> T extractImporterProviderServiceParameters(ImporterProviderService ImporterProviderService,
                                                                                                    HttpServletRequest request) {
        T importerProviderServiceParameters = createNewImporterProviderServiceParameters(ImporterProviderService)
        bindData(importerProviderServiceParameters, request)
        importerProviderServiceParameters
    }

}