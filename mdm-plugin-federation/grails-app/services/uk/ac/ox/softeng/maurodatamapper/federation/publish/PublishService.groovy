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
package uk.ac.ox.softeng.maurodatamapper.federation.publish

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class PublishService {

    AuthorityService authorityService

    @Autowired(required = false)
    List<ModelService> modelServices

    List<Model> findAllReadableModelsToPublish(UserSecurityPolicyManager userSecurityPolicyManager) {
        List<Model> models = []
        modelServices.each {
            List<Model> readableModels = it.findAllReadableModels(userSecurityPolicyManager, false, true, false)
            // Only publish finalised models which belong to this instance of MDM
            List<Model> publishableModels = readableModels.findAll {Model model ->
                model.finalised && model.authority.id == authorityService.getDefaultAuthority().id
            } as List<Model>
            models.addAll(publishableModels)
        }
        models
    }

    List<PublishedModel> findAllPublishedReadableModels(UserSecurityPolicyManager userSecurityPolicyManager) {
        findAllReadableModelsToPublish(userSecurityPolicyManager).collect {new PublishedModel(it)}
    }

    List<PublishedModel> findPublishedSupersedingModels(List<PublishedModel> publishedModels, String modelType, UUID modelId,
                                                        UserSecurityPolicyManager userSecurityPolicyManager) {
        List<VersionTreeModel> newerVersionTree =
            findServiceForModelDomainType(modelType).buildModelVersionTree(findModelByDomainTypeAndId(modelType, modelId), null, null,
                                                                           false, false, userSecurityPolicyManager)
        List<PublishedModel> newerPublishedModels = []
        def findPublishedParent
        findPublishedParent = {VersionTreeModel child ->
            VersionTreeModel parent = child.parentVersionTreeModel
            if (publishedModels.find {pm -> pm.modelId = parent.versionAware.id}) return parent
            else findPublishedParent(parent)
        }
        newerVersionTree.findAll {it ->
            Model model = it.versionAware
            model.id != modelId && publishedModels.find {pm -> pm.modelId == model.id}
        }.each {vtm ->
            newerPublishedModels << new PublishedModel(vtm.versionAware).tap {previousModelId = findPublishedParent(vtm).versionAware.id}
        }
        return newerPublishedModels
    }

    ModelService findServiceForModelDomainType(String domainType) {
        ModelService modelService = modelServices.find {it.handles(domainType)}
        if (!modelService) throw new ApiInternalException('MS01', "No model service to handle model [${domainType}]")
        return modelService
    }

    Model findModelByDomainTypeAndId(String domainType, UUID modelId) {
        return findServiceForModelDomainType(domainType).get(modelId)
    }
}
