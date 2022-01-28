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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.UniqueValuesValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.engine.search.predicate.IdSecureFilterFactory
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.binder.PathBinder
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Classifier implements Container {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers

    Classifier parentClassifier

    static hasMany = [
        childClassifiers: Classifier,
        metadata        : Metadata,
        annotations     : Annotation,
        semanticLinks   : SemanticLink,
        referenceFiles  : ReferenceFile,
        rules           : Rule
    ]

    static belongsTo = [
        Classifier
    ]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label unique: true
        metadata validator: {val, obj ->
            if (val) new UniqueValuesValidator('namespace:key').isValid(val.groupBy {"${it.namespace}:${it.key}"})
        }
    }

    static mapping = {
        childClassifiers cascade: 'all-delete-orphan'
        parentClassifier index: 'classifier_parent_classifier_idx', cascade: 'none'
    }

    static mappedBy = [
        childClassifiers: 'parentClassifier',
    ]

    static search = {
        label searchable: 'yes', analyzer: 'wordDelimiter'
        path valueBinder: new PathBinder()
        description termVector: 'with_positions'
        lastUpdated searchable: 'yes'
        dateCreated searchable: 'yes'
    }

    Classifier() {
        readableByAuthenticatedUsers = false
        readableByEveryone = false
    }

    @Override
    String getDomainType() {
        Classifier.simpleName
    }

    @Override
    String getPathPrefix() {
        'cl'
    }

    @Override
    String getPathIdentifier() {
        label
    }

    def beforeValidate() {
        childClassifiers.each {it.beforeValidate()}
    }

    @Override
    String getCreatedEditDescription() {
        parentClassifier ? "[${editLabel}] added as child of [${parentClassifier.editLabel}]" : "[$editLabel] created"
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

    @Override
    Container getParent() {
        parentClassifier
    }

    static DetachedCriteria<Classifier> by() {
        new DetachedCriteria<Classifier>(Classifier)
    }

    static DetachedCriteria<Classifier> byLabel(String label) {
        by().eq('label', label)
    }

    static DetachedCriteria<Classifier> byNoParentClassifier() {
        by().isNull('parentClassifier')
    }

    static DetachedCriteria<Classifier> byParentClassifierId(UUID id) {
        id ? by().eq('parentClassifier.id', id) : by().isNull('parentClassifier')
    }

    static DetachedCriteria<Classifier> byParentClassifierIdAndLabel(UUID id, String label) {
        byParentClassifierId(id).eq('label', label)
    }

    static List<Classifier> hibernateSearchList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        Classifier.search().list closure
    }

    static List<Classifier> treeLabelHibernateSearch(List<String> allowedIds, String searchTerm) {
        hibernateSearchList {
            keyword 'label', searchTerm
            filter IdSecureFilterFactory.createFilterPredicate(searchPredicateFactory, allowedIds)
        }
    }

    static List<Classifier> findAllContainedInClassifierPathNode(PathNode pathNode) {
        hibernateSearchList {
            should {
                keyword 'path', Path.from(pathNode)
            }
        }
    }

    static DetachedCriteria<Classifier> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<Classifier> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}