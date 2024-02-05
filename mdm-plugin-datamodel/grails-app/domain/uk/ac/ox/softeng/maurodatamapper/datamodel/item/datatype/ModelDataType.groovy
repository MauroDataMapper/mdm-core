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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.gorm.DetachedCriteria
import grails.rest.Resource
import grails.util.Holders
import org.grails.datastore.gorm.GormEntity

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelDataType extends DataType<ModelDataType> {

    UUID modelResourceId
    String modelResourceDomainType

    static constraints = {
    }

    ModelDataType() {
        domainType = ModelDataType.simpleName
    }

    ObjectDiff<ModelDataType> diff(ModelDataType otherDataType, String context) {
        diff(otherDataType, context, null, null)
    }

    ObjectDiff<ModelDataType> diff(ModelDataType otherDataType, String context, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        ObjectDiff<ModelDataType> diff = DiffBuilder.catalogueItemDiffBuilder(ModelDataType, this, otherDataType, lhsDiffCache, rhsDiffCache)

        // Aside from branch and version, is the model pointed to by the modelDataType really different by path?
        // Could be a different model entirely
        Model thisResourceModel = getModelResource(this)
        Model otherResourceModel = getModelResource(otherDataType)
        if (thisResourceModel && otherResourceModel) {
            if (!thisResourceModel.getPath().matches(otherResourceModel.getPath(), thisResourceModel.getPath().last().modelIdentifier)) {
                diff.
                    appendString('modelResourcePath',
                                 thisResourceModel.getPath().toString(),
                                 otherResourceModel.getPath().toString())
            }
        }
        diff
    }

    static Model getModelResource(ModelDataType modelDataType) {
        GrailsApplication grailsApplication = Holders.getGrailsApplication()
        Class<GormEntity> resourceClass = Utils.lookupGrailsDomain(grailsApplication, modelDataType.modelResourceDomainType)?.getClazz()
        if (resourceClass && modelDataType.modelResourceId) {
            Model model = resourceClass.get(modelDataType.modelResourceId)
            if (model) return model
            throw new ApiInternalException('MDT',
                                           'ModelDataType exists which points to a non-existent model ' +
                                           "${modelDataType.modelResourceDomainType}:${modelDataType.modelResourceId}")
        }
        return null
    }

    static DetachedCriteria<ModelDataType> byMetadataNamespaceAndKey(String metadataNamespace, String metadataKey) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
                eq 'key', metadataKey
            }
        }
    }

    static DetachedCriteria<ModelDataType> byMetadataNamespace(String metadataNamespace) {
        where {
            metadata {
                eq 'namespace', metadataNamespace
            }
        }
    }
}