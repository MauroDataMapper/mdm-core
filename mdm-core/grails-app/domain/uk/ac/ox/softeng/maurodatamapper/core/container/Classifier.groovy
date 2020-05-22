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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.search.PathTokenizerAnalyzer
import uk.ac.ox.softeng.maurodatamapper.search.bridge.OffsetDateTimeBridge
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Classifier implements Container {

    public final static Integer BATCH_SIZE = 1000

    UUID id

    Classifier parentClassifier

    static hasMany = [
        childClassifiers: Classifier,
    ]

    static belongsTo = [
        Classifier
    ]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label unique: true
        parentClassifier nullable: true
    }

    static mapping = {
        childClassifiers cascade: 'all-delete-orphan'
        parentClassifier index: 'classifier_parent_classifier_idx', cascade: 'none'
    }

    static mappedBy = [
        childClassifiers: 'parentClassifier',
    ]

    static search = {
        label index: 'yes', analyzer: 'wordDelimiter'
        path index: 'yes', analyzer: PathTokenizerAnalyzer
        description termVector: 'with_positions'
        lastUpdated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
        dateCreated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
    }

    Classifier() {
    }

    @Override
    String getDomainType() {
        Classifier.simpleName
    }


    @Override
    Classifier getPathParent() {
        parentClassifier
    }

    @Override
    def beforeValidate() {
        buildPath()
        childClassifiers.each {it.beforeValidate()}
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    void addCreatedEdit(User creator) {
        String description = parentClassifier ? "${editLabel} as child of [${parentClassifier.editLabel}]" : "${editLabel} added"
        addToEditsTransactionally creator, description
    }

    @Override
    String getEditLabel() {
        "Classifier:${label}"
    }

    @Override
    boolean hasChildren() {
        !childClassifiers.isEmpty()
    }

    @Override
    Boolean getDeleted() {
        false
    }

    static DetachedCriteria<Classifier> by() {
        new DetachedCriteria<Classifier>(Classifier)
    }

    static DetachedCriteria<Classifier> byLabel(String label) {
        by().eq('label', label)
    }

    static List<Classifier> luceneList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        Classifier.search().list closure
    }

    static List<Classifier> luceneTreeLabelSearch(List<String> allowedIds, String searchTerm) {
        luceneList {
            keyword 'label', searchTerm
            filter name: 'idSecured', params: [allowedIds: allowedIds]
        }
    }

    static List<Classifier> findAllContainedInClassifierId(UUID classifierId) {
        luceneList {
            should {
                keyword 'path', classifierId.toString()
            }
        }
    }
}