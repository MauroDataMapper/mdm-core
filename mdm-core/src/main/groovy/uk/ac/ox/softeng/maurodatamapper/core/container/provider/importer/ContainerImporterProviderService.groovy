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
package uk.ac.ox.softeng.maurodatamapper.core.container.provider.importer

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import org.springframework.core.GenericTypeResolver

@CompileStatic
abstract class ContainerImporterProviderService<C extends Container, P extends ImporterProviderServiceParameters> extends MauroDataMapperService {

    abstract ContainerService<C> getContainerService()

    abstract Boolean canImportMultipleDomains()

    abstract C importDomain(User currentUser, P params)

    abstract List<C> importDomains(User currentUser, P params)

    Class<P> getImporterProviderServiceParametersClass() {
        (Class<P>) GenericTypeResolver.resolveTypeArguments(getClass(), ContainerImporterProviderService).last()
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
    P createNewImporterProviderServiceParameters() throws ApiInternalException {
        try {
            return importerProviderServiceParametersClass.newInstance()
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new ApiInternalException('IP01', 'Cannot create new import params instance', ex)
        }
    }
}
