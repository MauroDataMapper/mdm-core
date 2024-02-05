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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.CodeSetFileImporterProviderServiceParameters

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindCodeSetImporterProviderService<T extends CodeSetFileImporterProviderServiceParameters> extends CodeSetImporterProviderService<T> {

    abstract CodeSet importCodeSet(User currentUser, byte[] content)

    List<CodeSet> importCodeSets(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple CodeSets")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindCodeSetImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    CodeSet importModel(User currentUser, CodeSetFileImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importCodeSet(currentUser, params.importFile.fileContents)
    }

    @Override
    List<CodeSet> importModels(User currentUser, CodeSetFileImporterProviderServiceParameters params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importCodeSets(currentUser, params.importFile.fileContents)
    }

    CodeSet bindMapToCodeSet(User currentUser, Map codeSetMap) {
        if (!codeSetMap) throw new ApiBadRequestException('FBIP03', 'No CodeSetMap supplied to import')

        CodeSet codeSet = new CodeSet()
        log.debug('Binding map to new CodeSet instance')
        DataBindingUtils.bindObjectToInstance(codeSet, codeSetMap, null, getImportBlacklistedProperties(), null)

        codeSetService.checkImportedCodeSetAssociations(currentUser, codeSet, codeSetMap)

        log.info('Import complete')
        codeSet
    }
}
