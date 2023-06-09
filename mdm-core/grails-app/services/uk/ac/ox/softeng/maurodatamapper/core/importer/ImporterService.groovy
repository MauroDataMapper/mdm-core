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
package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJob
import uk.ac.ox.softeng.maurodatamapper.core.async.AsyncJobService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config.ImportParameterConfig
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.importer.ImportParameterGroup
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.validation.ValidationErrors
import grails.web.databinding.DataBinder
import groovy.util.logging.Slf4j
import org.apache.commons.beanutils.PropertyUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.grails.datastore.gorm.GormValidateable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest

import java.lang.reflect.Field
import javax.servlet.http.HttpServletRequest

@Slf4j
@SuppressWarnings('GrUnnecessaryPublicModifier')
class ImporterService implements DataBinder {

    AsyncJobService asyncJobService
    VersionedFolderService versionedFolderService

    @Autowired
    MessageSource messageSource

    @Autowired(required = false)
    Set<ImporterProviderService> importerProviderServices

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> List<M> importDomains(
        User currentUser, T importer, P importParams) {
        importer.importDomains(currentUser, importParams).findAll()
    }

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> M importDomain(
        User currentUser, T importer, P importParams) {
        M model = importer.importDomain(currentUser, importParams)

        if (!model) throw new ApiBadRequestException('IS01', "Failed to import domain using ${importer.name} importer")
        model
    }

    public <M extends Model, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> AsyncJob asyncImportDomains(
        User currentUser, T importer, P importParams, ModelService modelService, Folder folder) {
        asyncJobService.createAndSaveAsyncJob("Import domains using ${importer.displayName}", currentUser.emailAddress) {
            List<M> models = importer.importDomains(currentUser, importParams).findAll()
            if (!models) {
                throw new ApiBadRequestException('IS01', "Failed to import domain using ${importer.name} importer")
            }

            if (!importParams.providerHasSavedModels) {
                if (versionedFolderService.isVersionedFolderFamily(folder) && models.any {it.finalised}) {
                    throw new ApiBadRequestException('IS02', 'Cannot import finalised models into a VersionedFolder')
                }

                models.each {m ->
                    m.folder = folder
                }

                modelService.validateMultipleModels(models)

                if (models.any {it.hasErrors()}) {
                    Errors errors = new ValidationErrors(models, models.first().class.getName())
                    models.findAll(it.hasErrors()).each {errors.addAllErrors((it as GormValidateable).errors)}
                    throw new ApiInvalidModelException('IS03', 'Invalid models', errors, messageSource)
                }
                log.debug('No errors in imported models')

                models.collect {modelService.saveAndAddSecurity(it, currentUser)}

                log.info('Multiple model save and import complete')
            } else {
                models.collect {modelService.addSecurity(it, currentUser)}
                log.info('Added security to multiple models')
            }
        }
    }

    public <M extends Model, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> AsyncJob asyncImportDomain(
        User currentUser, T importer, P importParams, ModelService modelService, Folder folder) {
        asyncJobService.createAndSaveAsyncJob("Import domain using ${importer.displayName}", currentUser.emailAddress) {
            M model = importer.importDomain(currentUser, importParams)
            if (!model) {
                throw new ApiBadRequestException('IS01', "Failed to import domain using ${importer.name} importer")
            }

            if (!importParams.providerHasSavedModels) {
                if (versionedFolderService.isVersionedFolderFamily(folder) && model.finalised) {
                    throw new ApiBadRequestException('IS02', 'Cannot import a finalised model into a VersionedFolder')
                }

                model.folder = folder

                modelService.validate(model)

                if (model.hasErrors()) {
                    throw new ApiInvalidModelException('IS04', 'Invalid model', (model as GormValidateable).errors, messageSource)
                }
                log.debug('No errors in imported model')

                modelService.saveAndAddSecurity(model, currentUser)

                log.info('Single model save and import complete')
            } else {
                modelService.addSecurity(it, currentUser)
                log.info('Added security to single model')
            }
        }
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

            // Only describe the parameters that arent hidden
            if (!config.hidden()) {

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
                    description: config.description().join(config.descriptionJoinDelimiter())
                )
                groups[group.name] = group
            }
        }

        groups.values().sort()
    }

    public <T extends ImporterProviderServiceParameters> Errors validateParameters(T paramsObj, Class<T> clazz) {
        Field[] fields = FieldUtils.getAllFields(clazz)
        Errors errors = new ValidationErrors(paramsObj, clazz.getName())
        for (Field field : fields) {
            ImportParameterConfig config = field.getAnnotation(ImportParameterConfig)
            if (config) {
                // Dont validate optional or hidden parameters
                if (config.optional() || config.hidden()) continue
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

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> List<T> findImporterProviderServicesByContentType(
        String contentType, Boolean canFederate = null) {
        importerProviderServices.findAll {it.handlesContentType(contentType) && (canFederate == null || it.canFederate() == canFederate)}.sort()
    }

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> T findImporterProviderServiceByContentType(
        String contentType, Boolean canFederate = null) {
        findImporterProviderServicesByContentType(contentType, canFederate)?.find()
    }

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> T findImporterProviderServiceByContentType(
        String namespace, String name, String version, String contentType, Boolean canFederate = null) {
        findImporterProviderServices(namespace, name, version).findAll {it.handlesContentType(contentType) && (canFederate == null || it.canFederate() == canFederate)}.sort()?.find()
    }

    public <M extends MdmDomain, P extends ImporterProviderServiceParameters, T extends ImporterProviderService<M, P>> List<T> findImporterProviderServices(String namespace,
                                                                                                                                                            String name,
                                                                                                                                                            String version) {
        if (version) {
            importerProviderServices.findAll {
                it.namespace.equalsIgnoreCase(namespace) &&
                it.name.equalsIgnoreCase(name) &&
                it.version.equalsIgnoreCase(version)
            }.sort()
        } else {
            importerProviderServices.findAll {
                it.namespace.equalsIgnoreCase(namespace) &&
                it.name.equalsIgnoreCase(name)
            }.sort()
        }
    }
}