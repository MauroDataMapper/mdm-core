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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.search.StandardSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator.TermCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class Terminology implements Model<Terminology> {

    UUID id

    Boolean hasChild

    static hasMany = [
        terms                : Term,
        termRelationshipTypes: TermRelationshipType,

        classifiers          : Classifier,
        metadata             : Metadata,
        annotations          : Annotation,
        semanticLinks        : SemanticLink,
        versionLinks         : VersionLink,
        referenceFiles       : ReferenceFile,
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        terms validator: {val, obj -> new TermCollectionValidator(obj).isValid(val)}
    }

    static mapping = {
        documentationVersion type: VersionUserType
        folder cascade: 'none'
        semanticLinks cascade: 'all-delete-orphan'
        versionLinks cascade: 'all-delete-orphan'
        breadcrumbTree cascade: 'all-delete-orphan', fetch: 'join'
        terms cascade: 'all-delete-orphan'
        termRelationshipTypes cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        metadata: 'none',
        terms   : 'terminology',
    ]

    static search = {
        CallableSearch.call(StandardSearch, delegate)
    }

    Terminology() {
        modelType = Terminology.simpleName
        documentationVersion = Version.from('1')
        finalised = false
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
    String getEditLabel() {
        "Terminology:${label}"
    }

    @Override
    Boolean hasChildren() {
        terms == null ? false : !terms.isEmpty()
    }

    ObjectDiff<Terminology> diff(Terminology otherTerminolgy) {
        modelDiffBuilder(Terminology, this, otherTerminolgy)
            .appendList(Term, 'terms', this.terms, otherTerminolgy.terms)
        //   .appendList(TermRelationshipType, 'termRelationshipTypes', this.termRelationshipTypes, otherTerminolgy.termRelationshipTypes)
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
        terms?.each {it.beforeValidate()}
    }

    int countTermsByCode(String code) {
        terms.count {it.code == code}
    }

    int countTermRelationshipTypesByLabel(String label) {
        termRelationshipTypes.count {it.label == label}
    }

    Term findTermByCode(String code) {
        terms.find {it.code == code}
    }

    TermRelationshipType findRelationshipTypeByLabel(String label) {
        termRelationshipTypes.find {it.label == label}
    }


    /*
     Boolean isTreeStructureCapable() {
            if (hasChild == null) {
                hasChild = TermRelationshipType.byTerminologyId(this.id).or {
                    eq('parentalRelationship', true)
                    eq('childRelationship', true)
                }.count() &&
                           Term.byTerminologyId(this.id)
                               .isNotNull('depth')
                               .lt('depth', Integer.MAX_VALUE)
            }
            hasChild
        }





        List<TermRelationship> getAllTermRelationships() {
            List<List<TermRelationship>> collection = terms?.collect {it.sourceTermRelationships ?: []} ?: []
            collection.flatten() as List<TermRelationship>
        }

        List<Term> getCodeSortedTerms() {

            terms.sort {a, b ->
                Double ln1 = TreeItem.extractNumber(a.code)
                Double ln2 = TreeItem.extractNumber(b.code)

                String ls1 = a.code.replaceAll(/[\d.]/, '')
                String ls2 = b.code.replaceAll(/[\d.]/, '')

                def res = ls1 <=> ls2
                if (res == 0) res = ln1 <=> ln2
                res
            }
        }
    */

    static DetachedCriteria<Terminology> by() {
        new DetachedCriteria<Terminology>(Terminology)
    }

    static DetachedCriteria<Terminology> byFolderId(UUID folderId) {
        by().eq('folder.id', folderId)
    }

    static DetachedCriteria<Terminology> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<Terminology> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        by().in('id', Metadata.byNamespaceAndKey(metadataNamespace, metadataKey).property('catalogueItem'))
    }

    static DetachedCriteria<Terminology> byMetadataNamespace(String metadataNamespace) {
        by().in('id', Metadata.byNamespace(metadataNamespace).property('catalogueItem'))
    }

    static DetachedCriteria<Terminology> byDeleted() {
        by().eq('deleted', true)
    }

    static DetachedCriteria<Terminology> byIdInList(Collection<UUID> ids) {
        by().inList('id', ids.toList())
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
