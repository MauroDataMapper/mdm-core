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
package uk.ac.ox.softeng.maurodatamapper.core.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEntity
import org.springframework.core.GenericTypeResolver

@CompileStatic
abstract class ImporterProviderService<D extends GormEntity, T extends ImporterProviderServiceParameters>
    extends MauroDataMapperService {

    abstract D importDomain(User currentUser, T params)

    abstract List<D> importDomains(User currentUser, T params)

    abstract Boolean canImportMultipleDomains()

    List<String> getImportBlacklistedProperties() {
        ['id', 'domainType', 'lastUpdated']
    }

    Class<T> getImporterProviderServiceParametersClass() {
        (Class<T>) GenericTypeResolver.resolveTypeArguments(getClass(), ImporterProviderService).last()
    }

    /**
     * Returns a new instance object of the defined ImporterProviderServiceParameters parameter.
     *
     * The method makes use of Class.newInstance(), this was deprecated in Java9 however is still part of the Groovy extension.
     * Due to the fact Groovy auto adds empty constructors to all its classes the Java9 method of getDeclaredConstructor.getNewInstance() fails
     * as there is no declared constructor.
     *
     * @return
     * @throws ApiInternalException
     */
    @SuppressWarnings('GrDeprecatedAPIUsage')
    T createNewImporterProviderServiceParameters() throws ApiInternalException {
        try {
            return importerProviderServiceParametersClass.newInstance()
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new ApiInternalException('IP01', 'Cannot create new import params instance', ex)
        }
    }

    @Override
    String getProviderType() {
        ProviderType.IMPORTER.name
    }
}
