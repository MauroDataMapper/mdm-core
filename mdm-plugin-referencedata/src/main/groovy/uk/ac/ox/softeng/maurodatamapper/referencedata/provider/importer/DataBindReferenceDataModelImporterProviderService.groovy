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
package uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValue
import uk.ac.ox.softeng.maurodatamapper.referencedata.provider.importer.parameter.ReferenceDataModelFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindReferenceDataModelImporterProviderService<T extends ReferenceDataModelFileImporterProviderServiceParameters> extends
    ReferenceDataModelImporterProviderService<T> {

    abstract ReferenceDataModel importReferenceDataModel(User currentUser, byte[] content)

    List<ReferenceDataModel> importReferenceDataModels(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple Reference Data Models")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindReferenceDataModelImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    List<ReferenceDataModel> importModels(User currentUser, ReferenceDataModelFileImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importReferenceDataModels(currentUser, params.importFile.fileContents)
    }

    @Override
    ReferenceDataModel importModel(User currentUser, ReferenceDataModelFileImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importReferenceDataModel(currentUser, params.importFile.fileContents)
    }

    ReferenceDataModel bindMapToReferenceDataModel(User currentUser, Map referenceDataModelMap) {
        if (!referenceDataModelMap) throw new ApiBadRequestException('FBIP03', 'No ReferenceDataModelMap supplied to import')

        log.debug('Setting map referenceDataTypes')
        referenceDataModelMap.referenceDataTypes = referenceDataModelMap.remove('referenceDataTypes')

        log.debug('Setting map referenceDataElements')
        referenceDataModelMap.referenceDataElements = referenceDataModelMap.remove('referenceDataElements')

        List mappedReferenceDataValues = referenceDataModelMap.remove('referenceDataValues')

        ReferenceDataModel referenceDataModel = new ReferenceDataModel()
        log.debug('Binding map to new ReferenceDataModel instance')
        DataBindingUtils.bindObjectToInstance(referenceDataModel, referenceDataModelMap, null, getImportBlacklistedProperties(), null)

        /**
         * Bind each Reference Data Value. Somehow the JsonSlurper and XmlSlurper produce different results. For Json, it is sufficient to do
         * referenceDataModelMap.referenceDataValues = referenceDataModelMap.remove('referenceDataValues')
         *
         * But when referenceDataModelMap has come from the XmlSlurper, using the line above does not set the referenceDataElement
         * attribute of each ReferenceDataValue.
         */
        mappedReferenceDataValues.each { rdv ->
            ReferenceDataElement referenceDataElement = new ReferenceDataElement()
            DataBindingUtils.bindObjectToInstance(referenceDataElement, rdv.referenceDataElement, null, getImportBlacklistedProperties(), null)

            ReferenceDataValue referenceDataValue = new ReferenceDataValue()
            DataBindingUtils.bindObjectToInstance(referenceDataValue, rdv, null, getImportBlacklistedProperties(), null)
            referenceDataValue.referenceDataElement = referenceDataElement

            referenceDataModel.addToReferenceDataValues(referenceDataValue)
        }


        log.debug('Fixing bound ReferenceDataModel')
        referenceDataModelService.checkImportedReferenceDataModelAssociations(currentUser, referenceDataModel, referenceDataModelMap)

        log.info('Import complete')
        referenceDataModel
    }
}