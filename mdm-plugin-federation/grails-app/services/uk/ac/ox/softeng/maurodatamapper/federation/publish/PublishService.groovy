/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.VersionTreeModel
import uk.ac.ox.softeng.maurodatamapper.federation.ExportLink
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.gorm.transactions.Transactional
import grails.web.mapping.LinkGenerator
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod

@Transactional
@Slf4j
class PublishService {

    AuthorityService authorityService

    @Autowired
    LinkGenerator linkGenerator

    @Autowired(required = false)
    List<ModelService> modelServices

    PublishedModel getPublishedModel(Model model, ModelService modelService) {
        String modelUrl = linkGenerator.link(resource: model, method: HttpMethod.GET, absolute: true)
        new PublishedModel(model).tap {
            exportLinks = modelService.getExporterProviderServices().collect {exporterProviderService ->
                new ExportLink(
                    url: new URL(modelUrl + '/export/' + exporterProviderService.namespace + '/' + exporterProviderService.name + '/' + exporterProviderService.version),
                    contentType: exporterProviderService.producesContentType,
                    isPreferred: exporterProviderService.isPreferred,
                    exporterProviderService: exporterProviderService)
            }
        }
    }

    List<PublishedModel> findAllPublishedReadableModels(UserSecurityPolicyManager userSecurityPolicyManager) {
        List<PublishedModel> publishedModels = []
        modelServices.each {modelService ->
            List<Model> readableModels = modelService.findAllReadableModels(userSecurityPolicyManager, false, true, false)
            // Only publish finalised models which belong to this instance of MDM
            List<Model> publishableModels = readableModels.findAll {Model model ->
                model.finalised && model.authority.id == authorityService.getDefaultAuthority().id
            }

            publishableModels.each {model ->
                publishedModels << getPublishedModel(model, modelService)
            }
        }
        publishedModels
    }

    List<PublishedModel> findPublishedSupersedingModels(List<PublishedModel> publishedModels, String modelType, UUID modelId,
                                                        UserSecurityPolicyManager userSecurityPolicyManager) {
        ModelService modelService = findServiceForModelDomainType(modelType)
        List<VersionTreeModel> newerVersionTree =
            modelService.buildModelVersionTree(findModelByDomainTypeAndId(modelType, modelId), null, null,
                                               false, false, userSecurityPolicyManager)
        List<PublishedModel> newerPublishedModels = []
        newerVersionTree.findAll {it ->
            Model model = it.versionAware
            model.id != modelId && publishedModels.find {pm -> pm.modelId == model.id}
        }.each {vtm ->
            newerPublishedModels << getPublishedModel(vtm.versionAware, modelService).tap {previousModelId = vtm.parentVersionTreeModel.versionAware.id}
        }
        return newerPublishedModels
    }

    private ModelService findServiceForModelDomainType(String domainType) {
        ModelService modelService = modelServices.find {it.handles(domainType)}
        if (!modelService) throw new ApiInternalException('MS01', "No model service to handle model [${domainType}]")
        return modelService
    }

    private Model findModelByDomainTypeAndId(String domainType, UUID modelId) {
        return findServiceForModelDomainType(domainType).get(modelId)
    }
}
