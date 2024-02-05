/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.search.ModelItemSearch
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.CallableSearch
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.gorm.constraint.validator.TermRelationshipTypeLabelValidator
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship

import com.google.common.base.CaseFormat
import grails.gorm.DetachedCriteria

class TermRelationshipType implements ModelItem<TermRelationshipType, Terminology> {

    public final static Integer BATCH_SIZE = 1000

    UUID id
    Terminology terminology

    String displayLabel
    Boolean parentalRelationship
    Boolean childRelationship

    static belongsTo = [Terminology]

    static hasMany = [
        classifiers      : Classifier,
        metadata         : Metadata,
        annotations      : Annotation,
        semanticLinks    : SemanticLink,
        referenceFiles   : ReferenceFile,
        termRelationships: TermRelationship,
        rules            : Rule
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        label validator: {val, obj -> new TermRelationshipTypeLabelValidator(obj).isValid(val)}
        displayLabel nullable: false
    }

    static mapping = {
        terminology index: 'term_relationship_type_terminology_idx', cascade: 'none', cascadeValidate: 'none'
    }

    static mappedBy = [
        termRelationships: 'relationshipType'
    ]

    static transients = ['aliases', 'model']

    static search = {
        CallableSearch.call(ModelItemSearch, delegate)
        modelId searchable: 'yes', indexingDependency: [reindexOnUpdate: 'shallow', derivedFrom: 'terminology']
    }

    TermRelationshipType() {
        parentalRelationship = false
        childRelationship = false
    }

    @Override
    String getDomainType() {
        TermRelationshipType.simpleName
    }

    @Override
    String getPathPrefix() {
        'trt'
    }

    UUID getModelId() {
        terminology.id
    }

    def beforeValidate() {
        if (!displayLabel) createDisplayLabel(label)
        beforeValidateModelItem()
    }

    String getEditLabel() {
        "TermRelationshipType:${label}"
    }

    @Override
    Terminology getModel() {
        terminology
    }

    @Override
    Boolean hasChildren() {
        false
    }

    ObjectDiff<TermRelationshipType> diff(TermRelationshipType that, String context) {
        diff(that, context, null, null)
    }

    ObjectDiff<TermRelationshipType> diff(TermRelationshipType that, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        DiffBuilder.catalogueItemDiffBuilder(TermRelationshipType, this, that, lhsDiffCache, rhsDiffCache)
            .appendString('displayLabel', this.displayLabel, that.displayLabel)
            .appendBoolean('parentalRelationship', this.parentalRelationship, that.parentalRelationship)
            .appendBoolean('childRelationship', this.childRelationship, that.childRelationship)
    }

    @Override
    Terminology getParent() {
        terminology
    }

    void createDisplayLabel(String label) {
        displayLabel = label
        if (!displayLabel) return
        // Replace all spaces and hyphens with underscores
        displayLabel = displayLabel.replaceAll(/[ \-]/, '_')

        // Convert all camel casing to underscores
        displayLabel = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, displayLabel)

        // Replace all underscores with spaces and trim to 1 space
        displayLabel = displayLabel.replaceAll(/_/, ' ').replaceAll(/ {2}/, ' ')

        // Capitalise each word in the label
        displayLabel.split().collect {it.capitalize()}.join(' ')
    }

    static DetachedCriteria<TermRelationshipType> by() {
        new DetachedCriteria<TermRelationshipType>(TermRelationshipType)
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyId(UUID terminologyId) {
        by().eq('terminology.id', terminologyId)
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyIdInList(Collection<UUID> terminologyIds) {
        by().inList('terminology.id', terminologyIds)
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyIdAndLabelIlikeOrDescriptionIlike(UUID terminologyId, String searchTerm) {
        byTerminologyId(terminologyId).or {
            ilike('label', "%${searchTerm}%")
            ilike('description', "%${searchTerm}%")
            ilike('displayLabel', "%${searchTerm}%")
        }
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyIdAndParentalRelationshipOrChildRelationship(UUID terminologyId) {
        byTerminologyId(terminologyId).or {
            eq('parentalRelationship', true)
            eq('childRelationship', true)
        }
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyIdAndId(UUID terminologyId, UUID id) {
        byTerminologyId(terminologyId).idEq(id)
    }

    static DetachedCriteria<TermRelationshipType> byTerminologyIdAndLabel(UUID terminologyId, String label) {
        byTerminologyId(terminologyId).eq('label', label)
    }

    static DetachedCriteria<TermRelationshipType> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }

    static DetachedCriteria<TermRelationshipType> withFilter(DetachedCriteria<TermRelationshipType> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.label}%")
        if (filters.description) criteria = criteria.ilike('description', "%${filters.description}%")
        criteria
    }
    static DetachedCriteria<TermRelationshipType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<TermRelationshipType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
