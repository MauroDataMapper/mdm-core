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
package uk.ac.ox.softeng.maurodatamapper.terminology.item

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator.TermCodeLabelValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator.TermRelationshipCollectionValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.validation.CascadeValidateType
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.FieldBridge
import org.hibernate.search.annotations.Index
import org.hibernate.search.bridge.builtin.UUIDBridge

@Resource(readOnly = false, formats = ['json', 'xml'])
class Term implements ModelItem<Term, Terminology> {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    Terminology terminology

    String code
    String definition
    String url
    Boolean isParent

    static belongsTo = [Terminology, CodeSet]

    static hasMany = [

        classifiers            : Classifier,
        metadata               : Metadata,
        annotations            : Annotation,
        semanticLinks          : SemanticLink,
        referenceFiles         : ReferenceFile,

        sourceTermRelationships: TermRelationship,
        targetTermRelationships: TermRelationship,

        codeSets               : CodeSet,
        rules                  : Rule
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        code nullable: false, blank: false, validator: {val, obj -> new TermCodeLabelValidator(obj).isValid(val)}
        sourceTermRelationships validator: {val, obj -> new TermRelationshipCollectionValidator().isValid(val)}
        url nullable: true, url: true
        isParent nullable: false
        definition nullable: false, blank: false
    }

    static mapping = {
        definition type: 'text'
        codeSets cascade: 'none', cascadeValidate: CascadeValidateType.NONE, index: 'jcstt_codeset_idx', joinTable: [
            name  : 'join_codeset_to_term',
            key   : 'term_id',
            column: 'codeSet_id'
        ]
        terminology index: 'term_terminology_idx', cascade: 'all-delete-orphan'
    }

    static mappedBy = [
        sourceTermRelationships: 'sourceTerm',
        targetTermRelationships: 'targetTerm',
    ]

    static transients = ['aliases', 'model']

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        code index: 'yes', analyzer: KeywordAnalyzer, sortable: [name: 'code_sort', normalizer: 'lowercase'], termVector: 'with_positions'
        definition termVector: 'with_positions'
    }

    Term() {
        // These values can only be changed by a service level check as they involve loading the terminology model into memory
        isParent = false
        depth = 1
    }

    @Override
    String getDomainType() {
        Term.simpleName
    }

    @Field(index = Index.YES, bridge = @FieldBridge(impl = UUIDBridge))
    UUID getModelId() {
        terminology.id
    }

    def beforeValidate() {
        label = code && definition && code == definition ? "${code}".toString() :
                code && definition ? "${code}: ${definition}".toString() :
                null
        buildTermPath()
        beforeValidateModelItem()
    }

    @Override
    def beforeInsert() {
        buildTermPath()
    }

    @Override
    def beforeUpdate() {
        buildTermPath()
    }

    @Override
    String getEditLabel() {
        "Term:${code}"
    }

    @Override
    Terminology getModel() {
        terminology
    }

    @Override
    Boolean hasChildren() {
        isParent
    }

    ObjectDiff<Term> diff(Term otherTerm) {
        catalogueItemDiffBuilder(Term, this, otherTerm)
            .appendList(TermRelationship, 'sourceTermRelationships', this.sourceTermRelationships, otherTerm.sourceTermRelationships)
            .appendList(TermRelationship, 'targetTermRelationships', this.targetTermRelationships, otherTerm.targetTermRelationships)
            .appendString('code', this.code, otherTerm.code)
            .appendString('definition', this.definition, otherTerm.definition)
            .appendString('url', this.url, otherTerm.url)
    }

    @Override
    GormEntity getPathParent() {
        terminology
    }

    void setCode(String code) {
        this.code = code
        this.label = this.code == this.definition ? "${this.code}" : "${this.code}: ${this.definition}"
    }

    void setDefinition(String definition) {
        this.definition = definition
        this.label = this.code == this.definition ? "${this.code}" : "${this.code}: ${this.definition}"
    }

    String buildTermPath() {
        // Override to ensure the depth is never touched
        if (terminology) {
            path = "/${terminology.ident().toString()}"
        } else {
            path = ''
        }
        if (breadcrumbTree) {
            if (!breadcrumbTree.matchesPath(path)) {
                breadcrumbTree.update(this)
            }

        } else {
            breadcrumbTree = new BreadcrumbTree(this)
        }
        path
    }

    @Override
    String buildPath() {
        // no-op
    }

    Term addToSourceTermRelationships(Map args) {
        addToSourceTermRelationships new TermRelationship(args)
    }

    Term addToTargetTermRelationships(Map args) {
        addToTargetTermRelationships new TermRelationship(args)
    }

    Term addToSourceTermRelationships(TermRelationship termRelationship) {
        termRelationship.targetTerm.addTo('targetTermRelationships', termRelationship)
        addTo('sourceTermRelationships', termRelationship)
    }

    Term addToTargetTermRelationships(TermRelationship termRelationship) {
        termRelationship.sourceTerm.addTo('sourceTermRelationships', termRelationship)
        addTo('targetTermRelationships', termRelationship)
    }

    static DetachedCriteria<Term> by() {
        new DetachedCriteria<Term>(Term)
    }

    static DetachedCriteria<Term> byTerminologyId(UUID terminologyId) {
        by().eq('terminology.id', terminologyId)
    }

    static DetachedCriteria<Term> byTerminologyIdAndHasChildDepth(UUID terminologyId) {
        byTerminologyId(terminologyId)
            .isNotNull('depth')
            .gt('depth', 1)
    }

    static DetachedCriteria<Term> byTerminologyIdAndCodeIlikeOrDefinitionIlike(UUID terminologyId, String searchTerm) {
        byTerminologyId(terminologyId).or {
            ilike('code', "%${searchTerm}%")
            ilike('definition', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<Term> byTerminologyIdAndCode(UUID terminologyId, String code) {
        byTerminologyId(terminologyId).eq('code', code)
    }

    static DetachedCriteria<Term> byTerminologyIdAndDepth(UUID terminologyId, Integer depth) {
        byTerminologyId(terminologyId).eq('depth', depth)
    }

    static DetachedCriteria<Term> byTerminologyIdAndId(UUID terminologyId, UUID id) {
        byTerminologyId(terminologyId).idEq(id)
    }

    static DetachedCriteria<Term> byCodeSetIdAndId(UUID codeSetId, UUID id) {
        byCodeSetId(codeSetId).idEq(id)
    }

    static DetachedCriteria<Term> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<Term> byCodeSetId(UUID codeSetId) {
        where {
            codeSets {
                eq 'id', codeSetId
            }
        }
    }

    static DetachedCriteria<Term> byTerminologyIdAndNotChild(UUID terminologyId) {
        byTerminologyId(terminologyId).not {
            and {
                sourceTermRelationships {
                    relationshipType {
                        eq('childRelationship', true)
                    }
                }
                targetTermRelationships {
                    relationshipType {
                        eq('parentalRelationship', true)
                    }
                }
            }
        }
    }

    static DetachedCriteria<Term> withFilter(DetachedCriteria<Term> criteria, Map filters) {
        if (filters.code) criteria = criteria.ilike('code', "%${filters.code}%")
        if (filters.definition) criteria = criteria.ilike('definition', "%${filters.definition}%")
        if (filters.terminologyLabel) criteria = criteria.where {
            terminology {
                ilike('label', "%${filters.terminologyLabel}%")
            }
        }
        criteria
    }

    /**
     * A path used when exporting as part of a CodeSet
     */
    String termPath() {
        "te:${terminology.label}|tm:${label}"
    }

    static DetachedCriteria<Term> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<Term> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
