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
package uk.ac.ox.softeng.maurodatamapper.datamodel.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.search.bridge.MetadataBridge
import uk.ac.ox.softeng.maurodatamapper.search.PipeTokenizerAnalyzer
import uk.ac.ox.softeng.maurodatamapper.search.bridge.OffsetDateTimeBridge

/**
 * @since 27/02/2020
 */
class StandardSearch {

    static search = {
        label index: 'yes', analyzer: 'wordDelimiter', sortable: [name: 'label_sort', normalizer: 'lowercase'], termVector: 'with_positions'
        description termVector: 'with_positions'
        //domainType index: 'yes', sortable: [name: 'domainType_sort', normalizer: 'lowercase']
        aliasesString index: 'yes', analyzer: PipeTokenizerAnalyzer
        metadata bridge: ['class': MetadataBridge]
        metadata indexEmbedded: true
        classifiers indexEmbedded: true
        lastUpdated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
        dateCreated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
    }
}
