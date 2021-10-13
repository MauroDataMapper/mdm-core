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
package uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiUnauthorizedException
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyFileImporterProviderServiceParameters

import grails.web.databinding.DataBindingUtils
import groovy.util.logging.Slf4j
import org.springframework.core.GenericTypeResolver

@Slf4j
abstract class DataBindTerminologyImporterProviderService<T extends TerminologyFileImporterProviderServiceParameters> extends TerminologyImporterProviderService<T> {

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

    Terminology importModel(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importTerminology(currentUser, params.importFile.fileContents)
    }

    List<Terminology> importModels(User currentUser, T params) {
        if (!currentUser) throw new ApiUnauthorizedException('FBIP01', 'User must be logged in to import model')
        if (params.importFile.fileContents.size() == 0) throw new ApiBadRequestException('FBIP02', 'Cannot import empty file')
        log.info('Importing {} as {}', params.importFile.fileName, currentUser.emailAddress)
        importTerminologies(currentUser, params.importFile.fileContents)
    }

    Terminology bindMapToTerminology(User currentUser, Map terminologyMap) {
        if (!terminologyMap) throw new ApiBadRequestException('FBIP03', 'No TerminologyMap supplied to import')

        Terminology terminology = new Terminology()
        log.debug('Binding map to new Terminology instance')
        DataBindingUtils.bindObjectToInstance(terminology, terminologyMap, null, getImportBlacklistedProperties(), null)

        bindTermRelationships(terminology, terminologyMap.termRelationships as List<Map<String, String>>)

        terminologyService.checkImportedTerminologyAssociations(currentUser, terminology)

        log.info('Import complete')
        terminology
    }

    void bindTermRelationships(Terminology terminology, List<Map<String, String>> termRelationships) {
        termRelationships.each {tr ->
            String sourceCode = tr.sourceTerm?.trim()
            String targetCode = tr.targetTerm?.trim()
            String relationshipType = tr.relationshipType?.trim()
            Term sourceTerm = terminology.findTermByCode(sourceCode)
            sourceTerm.addToSourceTermRelationships(new TermRelationship(
                relationshipType: terminology.findRelationshipTypeByLabel(relationshipType),
                targetTerm: terminology.findTermByCode(targetCode)
            ))
        }
    }
}
