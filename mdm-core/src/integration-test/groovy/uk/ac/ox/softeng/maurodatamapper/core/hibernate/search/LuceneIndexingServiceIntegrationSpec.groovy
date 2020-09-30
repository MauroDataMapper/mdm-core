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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

@Slf4j
@Integration
class LuceneIndexingServiceIntegrationSpec extends MdmSpecification {

    MessageSource messageSource
    LuceneIndexingService luceneIndexingService

    void 'test core lucene index directory'() {
        expect:
        luceneIndexingService.luceneIndexPath.toString().startsWith('/tmp/lucene/')
    }

    void 'test lucene default config mass indexer properties'() {
        expect:
        luceneIndexingService.massIndexerConfig == [typesToIndexInParallel: 1,
                                                    cacheMode             : 'IGNORE',
                                                    optimizeOnFinish      : true,
                                                    optimizeAfterPurge    : true,
                                                    purgeAllOnStart       : true,
                                                    transactionTimeout    : 1800,
                                                    threadsToLoadObjects  : 20,
                                                    batchSizeToLoadObjects: 1000,
                                                    idFetchSize           : -1]
    }
}
