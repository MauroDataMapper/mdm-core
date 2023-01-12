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
package uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.DataLoadable
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

/**
 * Created by james on 15/03/2017.
 */
@Slf4j
abstract class DataLoaderProviderService<T extends DataLoadable> extends MauroDataMapperService {

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    MessageSource messageSource

    abstract List<String> getClassifiers()

    abstract List<T> importData(Version version,
                                User catalogueUser)
        throws ApiException

    abstract String getAuthor()

    abstract String getOrganisation()

    abstract String getDescription()

    abstract Folder getFolder()

    abstract List<T> getPreviouslyImportedObjects()

    abstract Class<T> getImportClass()

    abstract void markUpNewData(List<T> importedData, Version dataLoaderVersion, User currentUser)

    abstract Boolean isInvalidModel(T importedData)

    abstract void supersedeOldData(List<T> previouslyImported, List<T> importedData,
                                   User currentUser)

    void validateAndSave(List<T> importedData) throws ApiInvalidModelException {
        if (importedData.size() > 1) log.debug('Performing validation of {} {}', importedData.count {it.hasChanged() || it.isDirty()},
                                               importClass.simpleName)
        long start = System.currentTimeMillis()
        importedData.each {
            if (it.hasChanged() || it.isDirty()) validateAndSave(it)
        }
        if (importedData.size() > 1) log.debug('Validate took: {}', Utils.getTimeString(
            System.currentTimeMillis() - start))
    }

    void save(List<T> importedData) throws ApiInvalidModelException {
        if (importedData.size() > 1) log.debug('Performing save of {} {}', importedData.count {it.hasChanged() || it.isDirty()},
                                               importClass.simpleName)
        long start = System.currentTimeMillis()
        importedData.each {
            if (it.hasChanged() || it.isDirty()) save(it)
        }
        if (importedData.size() > 1) log.debug('Save took: {}', Utils.getTimeString(
            System.currentTimeMillis() - start))
    }

    void validateAndSave(T importedData) throws ApiInvalidModelException {
        if (importedData.hasChanged() || importedData.isDirty()) {
            long start = System.currentTimeMillis()
            if (isInvalidModel(importedData)) {
                throw new ApiInvalidModelException('DLP01',
                                                   "${importClass.simpleName} ${importedData.label} failed validation with " +
                                                   "${importedData.errors.allErrors.size()} errors",
                                                   importedData.errors, messageSource)
            }

            log.debug('Validation of {}({}) took {}', importClass.simpleName, importedData.label,
                      Utils.getTimeString(System.currentTimeMillis() - start))
            save(importedData)
        } else log.debug('Not validating or saving as {}({}) has no changes', importClass.simpleName, importedData.label)
    }

    void save(T importedData) throws ApiInvalidModelException {
        if (importedData.hasChanged() || importedData.isDirty()) {

            long start = System.currentTimeMillis()
            importedData.save(failOnError: true, validate: false)
            sessionFactory.currentSession.flush()
            sessionFactory.currentSession.clear()
            log.debug('Performing save of {}({}) took {}', importClass.simpleName, importedData.label,
                      Utils.getTimeString(System.currentTimeMillis() - start))
        } else log.debug('Not saving as {}({}) has no changes', importClass.simpleName, importedData.label)
    }

    List<T> postImportActions(List<T> importedModels, User catalogueUser)
        throws ApiException {
        // no-op by default
        importedModels
    }

    @Override
    String getProviderType() {
        ProviderType.DATALOADER.name
    }
}
