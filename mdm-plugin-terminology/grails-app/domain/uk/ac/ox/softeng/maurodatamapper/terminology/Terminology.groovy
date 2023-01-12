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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator.TermCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class Terminology implements Model<Terminology> {

    UUID id

    Boolean hasChild

    TreeMap<String,Term> optimisedCodeSearchMap

    static hasMany = [
        terms                : Term,
        termRelationshipTypes: TermRelationshipType,

        classifiers          : Classifier,
        metadata             : Metadata,
        annotations          : Annotation,
        semanticLinks        : SemanticLink,
        versionLinks         : VersionLink,
        referenceFiles       : ReferenceFile,
        rules                : Rule
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases', 'optimisedCodeSearchMap']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        terms validator: {val, obj -> new TermCollectionValidator(obj).isValid(val)}
    }

    static mapping = {
        documentationVersion type: VersionUserType
        modelVersion type: VersionUserType
        folder cascade: 'none'
        authority fetch: 'join'
        terms cascade: 'all-delete-orphan'
        termRelationshipTypes cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        metadata: 'none',
        terms   : 'terminology',
    ]

    static search = {
        CallableSearch.call(ModelSearch, delegate)
    }

    Terminology() {
        initialiseVersioning()
        modelType = Terminology.simpleName
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        breadcrumbTree = new BreadcrumbTree(this)
        termRelationshipTypes = []
        terms = []
    }

    @Override
    String getDomainType() {
        Terminology.simpleName
    }

    @Override
    String getPathPrefix() {
        'te'
    }

    @Override
    String getEditLabel() {
        "Terminology:${label}"
    }

    ObjectDiff<Terminology> diff(Terminology otherTerminology, String context) {
        diff(otherTerminology, context, null, null)
    }

    ObjectDiff<Terminology> diff(Terminology otherTerminology, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        ObjectDiff<Terminology> base = DiffBuilder.modelDiffBuilder(Terminology, this, otherTerminology, lhsDiffCache, rhsDiffCache)

        if (!lhsDiffCache || !rhsDiffCache) {
            Set<TermRelationship> thisTermRelationships = (this.terms.collect {it.sourceTermRelationships ?: []}.flatten() +
                                                           this.terms.collect {it.targetTermRelationships ?: []}.flatten()).toSet() as Set<TermRelationship>
            Set<TermRelationship> otherTermRelationships = (otherTerminology.terms.collect {it.sourceTermRelationships ?: []}.flatten() +
                                                            otherTerminology.terms.collect {it.targetTermRelationships ?: []}.flatten()).toSet() as Set<TermRelationship>

            base
                .appendCollection(Term, 'terms', this.terms, otherTerminology.terms, Terminology.simpleName)
                .appendCollection(TermRelationshipType, 'termRelationshipTypes', this.termRelationshipTypes, otherTerminology.termRelationshipTypes)
                .appendCollection(TermRelationship, 'termRelationships', thisTermRelationships, otherTermRelationships)
        } else {
            base
                .appendCollection(Term, 'terms', Terminology.simpleName)
                .appendCollection(TermRelationshipType, 'termRelationshipTypes')
                .appendCollection(TermRelationship, 'termRelationships')
        }

        base
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
    }

    int countTermsByCode(String code) {
        terms.count {it.code == code}
    }

    int countTermRelationshipTypesByLabel(String label) {
        termRelationshipTypes.count {it.label == label}
    }

    Term findTermByCode(String code) {
        if(!optimisedCodeSearchMap || optimisedCodeSearchMap.size() != terms.size()) {
            optimisedCodeSearchMap = new TreeMap<>(terms.collectEntries {[it.code, it]})
        }
        optimisedCodeSearchMap[code]
    }

    TermRelationshipType findRelationshipTypeByLabel(String label) {
        termRelationshipTypes.find {it.label == label}
    }

    List<TermRelationship> getAllTermRelationships() {
        List<List<TermRelationship>> collection = terms?.collect {it.sourceTermRelationships ?: []} ?: []
        collection.flatten() as List<TermRelationship>
    }

    static DetachedCriteria<Terminology> by() {
        new DetachedCriteria<Terminology>(Terminology)
    }

    static DetachedCriteria<Terminology> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<Terminology> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<Terminology> withFilter(DetachedCriteria<Terminology> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.code}%")
        if (filters.code) criteria = criteria.ilike('code', "%${filters.code}%")
        if (filters.termLabel) criteria = criteria.where {
            terms {
                ilike('label', "%${filters.termLabel}%")
            }
        }
        criteria
    }
}
