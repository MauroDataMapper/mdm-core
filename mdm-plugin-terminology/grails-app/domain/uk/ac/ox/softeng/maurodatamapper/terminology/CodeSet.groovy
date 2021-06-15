/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.search.StandardSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.ParentOwnedLabelCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.hibernate.VersionUserType
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.mapping.validation.CascadeValidateType

@Resource(readOnly = false, formats = ['json', 'xml'])
class CodeSet implements Model<CodeSet> {

    UUID id

    static hasMany = [
        terms         : Term,

        classifiers   : Classifier,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        versionLinks  : VersionLink,
        referenceFiles: ReferenceFile,
        rules         : Rule
    ]

    static belongsTo = [Folder]

    static transients = ['hasChild', 'aliases']

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
        terms validator: {val, obj -> new ParentOwnedLabelCollectionValidator(obj, 'terms').isValid(val)}
        breadcrumbTree nullable: true
    }

    static mapping = {
        modelVersion type: VersionUserType
        documentationVersion type: VersionUserType
        folder cascade: 'none', cascadeValidate: CascadeValidateType.NONE
        authority fetch: 'join'
        terms cascade: 'none', cascadeValidate: CascadeValidateType.NONE, index: 'jcstt_term_idx', joinTable: [
            name  : 'join_codeset_to_term',
            key   : 'codeSet_id',
            column: 'term_id'
        ]
    }

    static mappedBy = [
        metadata: 'none',
        terms   : 'none'
    ]

    static search = {
        CallableSearch.call(StandardSearch, delegate)
    }

    CodeSet() {
        initialiseVersioning()
        modelType = CodeSet.simpleName
        deleted = false
        readableByAuthenticatedUsers = false
        readableByEveryone = false
    }

    @Override
    String getDomainType() {
        CodeSet.simpleName
    }

    @Override
    String getEditLabel() {
        "CodeSet:${label}"
    }

    ObjectDiff<CodeSet> diff(CodeSet otherCodeSet) {
        modelDiffBuilder(CodeSet, this, otherCodeSet)
            .appendList(Term, 'terms', this.terms, otherCodeSet.terms)
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
    }

    int countTermsByCode(String code) {
        terms.count {it.code == code}
    }

    /*
    Term findTermByCode(String code) {
        terms.find {it.code == code}
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

    static DetachedCriteria<CodeSet> by() {
        new DetachedCriteria<CodeSet>(CodeSet)
    }

    static DetachedCriteria<CodeSet> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<CodeSet> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }

    static DetachedCriteria<CodeSet> withFilter(DetachedCriteria<CodeSet> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('code', "%${filters.code}%")
        if (filters.code) criteria = criteria.ilike('code', "%${filters.code}%")
        if (filters.termLabel) criteria = criteria.where {
            terms {
                ilike('label', "%${filters.termLabel}%")
            }
        }
        criteria
    }

    CodeSet findByLabel(String label) {
        CodeSet.findByLabel(label)
    }
}
