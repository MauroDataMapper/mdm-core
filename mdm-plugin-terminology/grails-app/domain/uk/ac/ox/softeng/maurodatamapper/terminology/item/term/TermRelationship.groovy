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
package uk.ac.ox.softeng.maurodatamapper.terminology.item.term


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import org.grails.datastore.gorm.GormEntity

@Resource(readOnly = false, formats = ['json', 'xml'])
class TermRelationship implements ModelItem<TermRelationship, Terminology> {

    public final static Integer BATCH_SIZE = 5000

    UUID id

    static belongsTo = [sourceTerm      : Term,
                        targetTerm      : Term,
                        relationshipType: TermRelationshipType]

    static hasMany = [
        classifiers   : Classifier,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        referenceFiles: ReferenceFile,
        rules         : Rule
    ]

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        sourceTerm validator: {val, obj -> val == obj.targetTerm ? ['invalid.same.property.message', 'targetTerm'] : true}
    }

    static mapping = {
        sourceTerm index: 'term_relationship_source_term_idx', cascade: 'none'
        targetTerm index: 'term_relationship_target_term_idx', cascade: 'none'
    }

    static transients = ['aliases', 'model']

    @Override
    String getDomainType() {
        TermRelationship.simpleName
    }

    def beforeValidate() {
        label = relationshipType?.label
        beforeValidateModelItem()
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    String getEditLabel() {
        "TermRelationship:${relationshipType.label}"
    }

    @Override
    Terminology getModel() {
        sourceTerm?.terminology
    }

    @Override
    Boolean hasChildren() {
        false
    }

    @Override
    GormEntity getPathParent() {
        sourceTerm
    }

    boolean sourceIsParentToTarget() {
        relationshipType.parentalRelationship
    }

    boolean sourceIsChildOfTarget() {
        relationshipType.childRelationship
    }

    @Override
    String getDiffIdentifier() {
        if (!label) label = relationshipType?.label
        "$sourceTerm.label-$label-$targetTerm.label"
    }

    ObjectDiff<TermRelationship> diff(TermRelationship obj) {
        catalogueItemDiffBuilder(TermRelationship, this, obj)
    }

    static DetachedCriteria<TermRelationship> by() {
        new DetachedCriteria<TermRelationship>(TermRelationship).join('relationshipType')
    }

    static DetachedCriteria<TermRelationship> byTermId(UUID id) {
        byTermIdAndTermType(id, null)
    }

    static DetachedCriteria<TermRelationship> byTermIdAndId(UUID termId, UUID id) {
        byTermIdAndTermType(termId, null).idEq(id)
    }

    static DetachedCriteria<TermRelationship> byTermIdAndTermType(UUID id, String type) {
        def criteria = by()
        switch (type) {
            case 'source':
                criteria.eq 'sourceTerm.id', id
                break
            case 'target':
                criteria.eq 'targetTerm.id', id
                break
            default:
                criteria.or {
                    eq 'sourceTerm.id', id
                    eq 'targetTerm.id', id
                }
        }
        criteria
    }

    static DetachedCriteria<TermRelationship> bySourceTermId(UUID id) {
        byTermIdAndTermType(id, 'source')
    }

    static DetachedCriteria<TermRelationship> byTargetTermId(UUID id) {
        byTermIdAndTermType(id, 'target')
    }

    static DetachedCriteria<TermRelationship> byRelationshipTypeId(UUID relationshipTypeId) {
        new DetachedCriteria<TermRelationship>(TermRelationship).eq('relationshipType.id', relationshipTypeId)
    }

    static DetachedCriteria<TermRelationship> byRelationshipTypeIdAndId(UUID relationshipTypeId, UUID id) {
        new DetachedCriteria<TermRelationship>(TermRelationship).eq('relationshipType.id', relationshipTypeId).idEq(id)
    }

    static DetachedCriteria<TermRelationship> withFilter(DetachedCriteria<TermRelationship> criteria, Map filters) {
        if (filters.relationshipType) criteria = criteria.ilike('relationshipType.label', "%${filters.relationshipType}%")
        criteria
    }

    static DetachedCriteria<TermRelationship> byTermIdIsParent(UUID termId) {
        by().or {
            and {
                eq 'targetTerm.id', termId
                relationshipType {
                    eq('childRelationship', true)
                }
            }
            and {
                eq 'sourceTerm.id', termId
                relationshipType {
                    eq('parentalRelationship', true)
                }
            }
        }
    }

    static DetachedCriteria<TermRelationship> byTermIdIsChild(UUID termId) {
        by().or {
            and {
                eq 'sourceTerm.id', termId
                relationshipType {
                    eq('childRelationship', true)
                }
            }
            and {
                eq 'targetTerm.id', termId
                relationshipType {
                    eq('parentalRelationship', true)
                }
            }
        }
    }

    static DetachedCriteria<TermRelationship> byTermsAreParents(List<Term> terms) {
        by().or {

            and {
                inList('targetTerm', terms)
                relationshipType {
                    eq('childRelationship', true)
                }
            }
            and {
                inList('sourceTerm', terms)
                relationshipType {
                    eq('parentalRelationship', true)
                }
            }
        }
    }

    static DetachedCriteria<TermRelationship> bySourceTermIdAndParental(UUID id) {
        bySourceTermId(id).where {
            relationshipType {
                eq('parentalRelationship', true)
            }
        }
    }

    static DetachedCriteria<TermRelationship> byTargetTermIdAndChild(UUID id) {
        byTargetTermId(id).where {
            relationshipType {
                eq('childRelationship', true)
            }
        }
    }

    static DetachedCriteria<TermRelationship> byTermIdHasHierarchy(UUID termId) {
        byTermId(termId).where {
            or {
                relationshipType {
                    eq('childRelationship', true)
                }
                relationshipType {
                    eq('parentalRelationship', true)
                }
            }
        }
    }

    static DetachedCriteria<TermRelationship> byClassifierId(UUID classifierId) {
        where {
            classifiers {
                eq 'id', classifierId
            }
        }
    }
    static DetachedCriteria<TermRelationship> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<TermRelationship> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}
