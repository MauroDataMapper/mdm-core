/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * @since 16/11/2017
 */
@Slf4j
@CompileStatic
abstract class ExporterProviderService extends MauroDataMapperService {

    abstract ByteArrayOutputStream exportDomain(User currentUser, UUID domainId) throws ApiException

    abstract ByteArrayOutputStream exportDomains(User currentUser, List<UUID> domainIds)
        throws ApiException

    abstract Boolean canExportMultipleDomains()

    abstract String getFileExtension()

    abstract String getFileType()

    /**
     * MIME type produced by ExporterProviderService
     * @return MIME type
     */
    String getProducesContentType() {
        null
    }

    /**
     * Flag preferred/native ExporterProviderService, to be sorted first.
     * @return true if preferred
     */
    Boolean getIsPreferred() {
        false
    }

    @Override
    String getProviderType() {
        ProviderType.EXPORTER.name
    }

    @Override
    int compareTo(MauroDataMapperService that) {
        if (that instanceof ExporterProviderService) {
            int result = that.isPreferred <=> this.isPreferred
            if (result == 0) result = this.order <=> that.order
            if (result == 0) result = super.compareTo(that)
            return result
        } else {
            return super.compareTo(that)
        }
    }
}