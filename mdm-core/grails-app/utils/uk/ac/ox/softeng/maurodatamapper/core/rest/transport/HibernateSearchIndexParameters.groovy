/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.rest.transport

import grails.validation.Validateable

/**
 * @since 26/04/2018
 */
class HibernateSearchIndexParameters implements Validateable {

    Integer typesToIndexInParallel
    Integer threadsToLoadObjects
    Integer batchSizeToLoadObjects
    String cacheMode
    Boolean optimizeOnFinish
    Boolean optimizeAfterPurge
    Boolean purgeAllOnStart
    Integer idFetchSize
    Integer transactionTimeout

    static constraints = {
        //        username nullable: false, email: true
        //        password nullable: false
        typesToIndexInParallel nullable: true, min: 0
        threadsToLoadObjects nullable: true, min: 0
        batchSizeToLoadObjects nullable: true, min: 0
        cacheMode nullable: true
        optimizeOnFinish nullable: true
        optimizeAfterPurge nullable: true
        purgeAllOnStart nullable: true
        idFetchSize nullable: true, min: 0
        transactionTimeout nullable: true, min: 0
    }

    void updateFromMap(Map map) {
        typesToIndexInParallel = typesToIndexInParallel ?: map.typesToIndexInParallel as Integer
        threadsToLoadObjects = threadsToLoadObjects ?: map.threadsToLoadObjects as Integer
        batchSizeToLoadObjects = batchSizeToLoadObjects ?: map.batchSizeToLoadObjects as Integer
        cacheMode = cacheMode ?: map.cacheMode
        optimizeOnFinish = optimizeOnFinish ?: map.optimizeOnFinish
        optimizeAfterPurge = optimizeAfterPurge ?: map.optimizeAfterPurge
        purgeAllOnStart = purgeAllOnStart ?: map.purgeAllOnStart
        idFetchSize = idFetchSize ?: map.idFetchSize as Integer
        transactionTimeout = transactionTimeout ?: map.transactionTimeout as Integer
    }

    @Override
    String toString() {
        'HS Mass Indexer Parameters:\n' +
        "  typesToIndexInParallel: $typesToIndexInParallel\n" +
        "  threadsToLoadObjects: $threadsToLoadObjects\n" +
        "  batchSizeToLoadObjects: $batchSizeToLoadObjects\n" +
        "  cacheMode: $cacheMode\n" +
        "  optimizeOnFinish: $optimizeOnFinish\n" +
        "  optimizeAfterPurge: $optimizeAfterPurge\n" +
        "  purgeAllOnStart: $purgeAllOnStart\n" +
        "  idFetchSize: $idFetchSize\n" +
        "  transactionTimeout: $transactionTimeout"
    }
}
