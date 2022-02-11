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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory

@Slf4j
@Transactional
class BreadcrumbTreeService {

    SessionFactory sessionFactory

    def finalise(BreadcrumbTree breadcrumbTree) {
        if (!breadcrumbTree) return
        log.trace('Finalising BreadcrumbTree')
        long start = System.currentTimeMillis()
        breadcrumbTree.finalised = true
        String treeStringBefore = breadcrumbTree.treeString
        breadcrumbTree.buildTree()
        breadcrumbTree.path = breadcrumbTree.domainEntity.path

        // Update all the modelitem tree strings
        String treeStringAfter = breadcrumbTree.treeString
        String treeStringLike = "${treeStringBefore}%"
        sessionFactory.currentSession.createSQLQuery('UPDATE core.breadcrumb_tree ' +
                                                     'SET tree_string = REPLACE(tree_string, :treeStringBefore, :treeStringAfter) ' +
                                                     'WHERE tree_string LIKE :treeStringLike')
            .setParameter('treeStringBefore', treeStringBefore)
            .setParameter('treeStringAfter', treeStringAfter)
            .setParameter('treeStringLike', treeStringLike)
            .executeUpdate()
        breadcrumbTree.save()
        log.trace('BT finalisation took {}', Utils.timeTaken(start))
    }
}
