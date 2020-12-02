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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindTerminologyImporterProviderService<T extends TerminologyFileImporterProviderServiceParameters> extends
    TerminologyImporterProviderService<T> {

    abstract Terminology importTerminology(User currentUser, byte[] content)

    List<Terminology> importTerminologies(User currentUser, byte[] content) {
        throw new ApiBadRequestException('FBIP04', "${getName()} cannot import multiple Terminologies")
    }

    @Override
    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArgument(getClass(), DataBindTerminologyImporterProviderService)
    }

    @Override
    Boolean canImportMultipleDomains() {
        false
    }

    @Override
    List<Terminology> importTerminologies(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        List<Terminology> imported = importTerminologies(currentUser, params.importFile.fileContents)
        imported.collect {updateImportedModelFromParameters(it, params, true)}
    }

    @Override
    Terminology importTerminology(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        Terminology imported = importTerminology(currentUser, params.importFile.fileContents)
        updateImportedModelFromParameters(imported, params)
    }

    Terminology updateImportedModelFromParameters(Terminology terminology, T params, boolean list = false) {
        if (params.finalised != null) terminology.finalised = params.finalised
        if (!list && params.modelName) terminology.label = params.modelName
        terminology
    }

    Terminology bindMapToTerminology(User currentUser, Map terminologyMap) {
        if (!terminologyMap) throw new ApiBadRequestException('FBIP03', 'No TerminologyMap supplied to import')

        Terminology terminology = new Terminology()
        log.debug('Binding map to new Terminology instance')
        DataBindingUtils.bindObjectToInstance(terminology, terminologyMap, null, ['id', 'domainType', 'lastUpdated'], null)

        bindTermRelationships(terminology, terminologyMap.termRelationships)

        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)

        log.info('Import complete')
        terminology
    }

    void bindTermRelationships(Terminology terminology, List<Map> termRelationships) {
        termRelationships.each {tr ->
            Term sourceTerm = terminology.findTermByCode(tr.sourceTerm)
            sourceTerm.addToSourceTermRelationships(new TermRelationship(
                relationshipType: terminology.findRelationshipTypeByLabel(tr.relationshipType),
                targetTerm: terminology.findTermByCode(tr.targetTerm)
            ))
        }
    }
}