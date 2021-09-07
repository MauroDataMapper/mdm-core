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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
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
        modelResourceDomainType validator: {source, obj ->
            if (source == DataModel.simpleName) ['invalid.model.type.datatype']
        }
    }

    ModelDataType() {
        domainType = ModelDataType.simpleName
    }

    ObjectDiff<ModelDataType> diff(ModelDataType otherDataType, String context) {
        ObjectDiff<ModelDataType> diff = catalogueItemDiffBuilder(ModelDataType, this, otherDataType)

        // Aside from branch and version, is the model pointed to by the modelDataType really different by path?
        // Could be a different model entirely
        Model thisResourceModel = getModelResource(this)
        Model otherResourceModel = getModelResource(otherDataType)
        if (thisResourceModel && otherResourceModel) {
            Path thisResourcePath = Path.from(thisResourceModel)
            Path otherResourcePath = Path.from(otherResourceModel)
            if (!thisResourcePath.matches(otherResourcePath, thisResourcePath.last().modelIdentifier)) {
                diff.
                    appendString('modelResourcePath',
                                 makeFullyQualifiedPath(thisResourceModel).toString(),
                                 makeFullyQualifiedPath(otherResourceModel).toString())
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
                                           "ModelDataType exists which points to a non-existent model " +
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

    /**
     * Make a full qualified path by recursing up through parents. Firstly look at the folder
     * to which the Model belongs, then any hierarchy of parent folders.
     * @param model
     * @return Path of the model, including all parents
     */
    static Path makeFullyQualifiedPath(Model model) {
        List<MdmDomain> nodes = []
        nodes << model
        nodes << model.folder
        folderParents(nodes, model.folder)

        return Path.from(nodes.reverse())
    }

    static folderParents(List<MdmDomain> nodes, Folder folder) {
        if (folder.parentFolder) {
            nodes << folder.parentFolder
            folderParents(nodes, folder.parentFolder)
        }
    }
}