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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware

import grails.validation.Validateable
import groovy.util.logging.Slf4j

@Slf4j
class CopyInformation implements Validateable {

    String copyLabel
    boolean copyIndex = false

    Map<String, TreeMap<UUID, List<MultiFacetItemAware>>> preloadedFacets = [:]

    static constraints = {
        copyLabel blank: false
    }

    // Allow facets to be preloaded from the db and passed in via the copy information
    // Facets loaded in this way could be more than just those belonging to the item being copied so we need to extract only those relevant
    public <K extends MultiFacetItemAware> List<K> extractPreloadedFacetsForTypeAndId(Class<K> clazz, String mapKey, UUID multiFacetAwareItemId) {
        TreeMap<UUID, List<MultiFacetItemAware>> groupedFacets = preloadedFacets[mapKey]
        if (!groupedFacets) return []
        groupedFacets[multiFacetAwareItemId] as List<K>
    }

    boolean hasFacetData(String mapKey) {
        preloadedFacets.containsKey(mapKey)
    }
}
