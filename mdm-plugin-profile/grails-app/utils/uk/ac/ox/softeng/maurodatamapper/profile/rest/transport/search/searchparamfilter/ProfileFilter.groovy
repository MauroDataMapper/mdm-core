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
package uk.ac.ox.softeng.maurodatamapper.profile.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.mapper.pojo.bridge.MetadataBridge
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter

import grails.plugins.hibernate.search.HibernateSearchApi

/**
 * @since 13/04/2022
 */
class ProfileFilter implements SearchParamFilter {

    @Override
    boolean doesApply(SearchParams searchParams) {
        searchParams.containsKey('profileFields')
    }

    @Override
    Closure getClosure(SearchParams searchParams) {
        List<Map<String, String>> profileFields = searchParams.getValue('profileFields')
        HibernateSearchApi.defineSearchQuery {
            must {
                profileFields.each {field ->
                    String namespace = field.metadataNamespace
                    String key = field.metadataPropertyName
                    String value = field.filterTerm
                    phrase(MetadataBridge.makeSafeFieldName("${namespace}|${key}"), value)
                }
            }
        }
    }
}
