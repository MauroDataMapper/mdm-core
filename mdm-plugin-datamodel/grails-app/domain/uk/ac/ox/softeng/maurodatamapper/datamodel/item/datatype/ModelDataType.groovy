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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.util.Holders
import grails.gorm.DetachedCriteria
import grails.rest.Resource

//@SuppressFBWarnings('HE_INHERITS_EQUALS_USE_HASHCODE')
@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelDataType extends DataType<ModelDataType> {

    UUID modelResourceId
    String modelResourceDomainType

    static constraints = {
        modelResourceDomainType validator: { source, obj ->
            if (source == DataModel.simpleName) ['invalid.model.type.datatype']
        }
    }

    ModelDataType() {
        domainType = ModelDataType.simpleName
    }

    ObjectDiff<ModelDataType> diff(ModelDataType otherDataType, String context) {
        ObjectDiff<ModelDataType> diff = catalogueItemDiffBuilder(ModelDataType, this, otherDataType)

        GrailsApplication grailsApplication = Holders.getGrailsApplication()

        Class thisResourceClass = Utils.lookupGrailsDomain(grailsApplication, this.modelResourceDomainType)?.getClazz()
        Class otherResourceClass = Utils.lookupGrailsDomain(grailsApplication, otherDataType.modelResourceDomainType)?.getClazz()

        if (thisResourceClass && otherResourceClass) {
            List<Model> thisResourceModels = thisResourceClass.byIdInList([this.modelResourceId]).list()
            List<Model> otherResourceModels = otherResourceClass.byIdInList([otherDataType.modelResourceId]).list()

            if (thisResourceModels.size() == 1 && otherResourceModels.size() == 1) {
                Model thisResourceModel = thisResourceModels.first()
                Model otherResourceModel = otherResourceModels.first()

                // Aside from branch and version, is the model pointed to by the modelDataType really different by path?
                Path thisResourcePath = Path.from(thisResourceModel.folder, thisResourceModel)
                Path otherResourcePath = Path.from(otherResourceModel.folder, otherResourceModel)
                if (!thisResourcePath.matches(otherResourcePath, thisResourceModel.modelIdentifier)) {
                    diff.
                    appendString('modelResourcePath',
                    makeFullyQualifiedPath(thisResourceModel).toString(),
                    makeFullyQualifiedPath(otherResourceModel).toString())
                }
            }
        }

        diff
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
        List<CreatorAware> nodes = []
        nodes << model

        if (model.folder) {
            nodes << model.folder
            folderParents(nodes, model.folder)
        }

        return Path.from(nodes.reverse())
    }

    static folderParents(List<CreatorAware> nodes, Folder folder) {
        if (folder.parentFolder) {
            nodes << folder.parentFolder
            folderParents(nodes, folder.parentFolder)
        }
    }
}