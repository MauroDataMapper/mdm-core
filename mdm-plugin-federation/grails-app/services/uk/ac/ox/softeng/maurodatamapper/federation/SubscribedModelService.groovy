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
package uk.ac.ox.softeng.maurodatamapper.federation

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.importer.ImporterService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.federation.rest.transport.SubscribedModelFederationParams
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.transactions.Transactional
import grails.rest.Link
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

import java.time.OffsetDateTime

@Slf4j
@Transactional
class SubscribedModelService implements SecurableResourceService<SubscribedModel>, AnonymisableService {

    @Autowired(required = false)
    List<MdmDomainService> domainServices

    SubscribedCatalogueService subscribedCatalogueService
    FolderService folderService
    ImporterService importerService

    @Autowired(required = false)
    SecurityPolicyManagerService securityPolicyManagerService

    @Override
    SubscribedModel get(Serializable id) {
        SubscribedModel.get(id)
    }

    @Override
    List<SubscribedModel> getAll(Collection<UUID> containerIds) {
        SubscribedModel.getAll(containerIds)
    }

    SubscribedModel findBySubscribedCatalogueIdAndSubscribedModelId(UUID subscribedCatalogueId, String subscribedModelId) {
        SubscribedModel.bySubscribedCatalogueIdAndSubscribedModelId(subscribedCatalogueId, subscribedModelId).get()
    }

    SubscribedModel findBySubscribedCatalogueIdAndId(UUID subscribedCatalogueId, UUID id) {
        SubscribedModel.bySubscribedCatalogueIdAndId(subscribedCatalogueId, id).get()
    }

    List<SubscribedModel> list(Map pagination = [:]) {
        SubscribedModel.list(pagination)
    }

    List<SubscribedModel> findAllBySubscribedCatalogueId(UUID subscribedCatalogueId, Map pagination = [:]) {
        SubscribedModel.bySubscribedCatalogueId(subscribedCatalogueId).list(pagination)
    }

    @Override
    List<SubscribedModel> findAllReadableByEveryone() {
        SubscribedModel.findAllByReadableByEveryone(true)
    }

    @Override
    List<Authority> findAllReadableByAuthenticatedUsers() {
        SubscribedModel.findAllByReadableByAuthenticatedUsers(true)
    }

    Long count() {
        SubscribedModel.count()
    }

    @Override
    void delete(SubscribedModel subscribedModel) {
        subscribedModel.delete(flush: true)
        if (securityPolicyManagerService) {
            securityPolicyManagerService.removeSecurityForSecurableResource(subscribedModel, null)
        }
    }

    @Override
    boolean handles(Class clazz) {
        clazz == SubscribedModel
    }

    @Override
    boolean handles(String domainType) {
        domainType == SubscribedModel.simpleName
    }

    SubscribedModel save(SubscribedModel subscribedModel) {
        subscribedModel.save(failOnError: true, validate: false)
    }

    def federateSubscribedModel(SubscribedModelFederationParams subscribedModelFederationParams, UserSecurityPolicyManager userSecurityPolicyManager) {
        SubscribedModel subscribedModel = subscribedModelFederationParams.subscribedModel
        Folder folder = folderService.get(subscribedModel.folderId)

        log.debug('Exporting SubscribedModel')
        try {
            List<Link> exportLinks = getExportLinksForSubscribedModel(subscribedModel)

            if (!exportLinks.find {link ->
                (!subscribedModelFederationParams.url || subscribedModelFederationParams.url == link.href) &&
                (!subscribedModelFederationParams.contentType || subscribedModelFederationParams.contentType == link.contentType)
            }) {
                log.debug('No published link found for specified URL and/or content type')
                subscribedModel.errors.reject('invalid.subscribedmodel.import.link.notfound',
                                              [subscribedModelFederationParams.url, subscribedModelFederationParams.contentType].toArray(),
                                              'Could not import SubscribedModel into local Catalogue, no published link found for URL [{0}] and/or content type [{1}]')
                return subscribedModel.errors
            }

            ImporterProviderService importerProviderService
            Link exportLink
            if (subscribedModelFederationParams.importerProviderService?.namespace && subscribedModelFederationParams.importerProviderService?.name) {
                exportLink = exportLinks.find {link ->
                    (!subscribedModelFederationParams.url || subscribedModelFederationParams.url == link.href) &&
                    (!subscribedModelFederationParams.contentType || subscribedModelFederationParams.contentType == link.contentType) &&
                    (importerProviderService =
                        importerService.findImporterProviderServiceByContentType(subscribedModelFederationParams.importerProviderService.namespace,
                                                                                 subscribedModelFederationParams.importerProviderService.name,
                                                                                 subscribedModelFederationParams.importerProviderService.version, link.contentType))
                }
            } else {
                exportLink = exportLinks.find {link ->
                    (!subscribedModelFederationParams.url || subscribedModelFederationParams.url == link.href) &&
                    (!subscribedModelFederationParams.contentType || subscribedModelFederationParams.contentType == link.contentType) &&
                    (importerProviderService = importerService.findImporterProviderServiceByContentType(link.contentType))
                }
            }

            if (!importerProviderService) {
                log.debug('No ImporterProviderService found for any published content type and given parameters')
                subscribedModel.errors.reject('invalid.subscribedmodel.import.format.unsupported',
                                              'Could not import SubscribedModel into local Catalogue, cannot find compatible export link and importer using given parameters')
                return subscribedModel.errors
            }

            ModelImporterProviderServiceParameters parameters = importerService.createNewImporterProviderServiceParameters(importerProviderService)

            if (parameters.hasProperty('importFile')?.type != FileParameter) {
                throw new ApiInternalException('SMS01', "Importer ${importerProviderService.class.simpleName} " +
                                                        'for model cannot import file content')
            }

            byte[] resourceBytes = exportSubscribedModelFromSubscribedCatalogue(subscribedModel, exportLink)

            parameters.importFile = new FileParameter(fileContents: resourceBytes)
            parameters.folderId = folder.id
            parameters.finalised = true
            parameters.useDefaultAuthority = false

            MdmDomain model = importerService.importDomain(userSecurityPolicyManager.user, importerProviderService, parameters)

            if (!model) {
                subscribedModel.errors.reject('invalid.subscribedmodel.import',
                                              'Could not import SubscribedModel into local Catalogue')
                return subscribedModel.errors
            } else if (!Model.isAssignableFrom(model.class) && !VersionedFolder.isAssignableFrom(model.class)) {
                throw new ApiInternalException('SMS02', "Domain type ${model.domainType} cannot be imported")
            }

            log.debug('Importing domain {}, version {} from authority {}', model.label, model.modelVersion, model.authority)
            MdmDomainService domainService = domainServices.find {it.handles(model.domainType)}
            if (domainService.respondsTo('countByAuthorityAndLabelAndVersion') &&
                domainService.countByAuthorityAndLabelAndVersion(model.authority, model.label, model.modelVersion)) {
                subscribedModel.errors.reject('invalid.subscribedmodel.import.already.exists',
                                              [model.authority, model.label, model.modelVersion].toArray(),
                                              'Model from authority [{0}] with label [{1}] and version [{2}] already exists in catalogue')
                return subscribedModel.errors
            }

            if (!model.hasProperty('folder')) {
                throw new ApiInternalException('SMS03', "Domain type ${model.domainType} cannot be imported into a Folder")
            }
            model.folder = folder

            domainService.validate(model)

            if (model.hasErrors()) {
                return model.errors
            }

            MdmDomain savedModel
            if (domainService.respondsTo('saveModelWithContent')) {
                savedModel = domainService.saveModelWithContent(model)
            } else {
                savedModel = domainService.save(model)
            }
            log.debug('Saved domain')

            if (securityPolicyManagerService) {
                log.debug('add security to saved model')
                userSecurityPolicyManager = securityPolicyManagerService.addSecurityForSecurableResource(savedModel,
                                                                                                         userSecurityPolicyManager.user,
                                                                                                         savedModel.label)
            }

            //Record the ID of the imported model against the subscription makes it easier to track version links later.
            subscribedModel.lastRead = OffsetDateTime.now()
            subscribedModel.localModelId = savedModel.id
            subscribedModel.subscribedModelType = savedModel.domainType

            //Handle version linking
            if (domainService.respondsTo('getUrlResourceName')) {
                Map versionLinks = getVersionLinks(domainService.getUrlResourceName(), subscribedModel)
                if (versionLinks) {
                    log.debug('add version links')
                    addVersionLinksToImportedModel(userSecurityPolicyManager.user, versionLinks, domainService, subscribedModel)
                }
            }

        } catch (ApiException exception) {
            log.warn("Failed to federate subscribedModel due to [${exception.message}]")
            subscribedModel.errors.reject('invalid.subscribedmodel.federate.exception',
                                          [exception.message].toArray(),
                                          'Could not federate SubscribedModel into local Catalogue due to [{0}]')
            return subscribedModel.errors
        }

        subscribedModel
    }

    /**
     * Get version links from the subscribed catalogue for the specified subscribed model
     * 1. Create a URL for {modelDomainType}/{modelId}/versionLinks
     * 2. Slurp the Json returned by this endpoint and return the object
     *
     * @param urlModelResourceType
     * @param subscribedModel
     * @return A map of slurped version links
     */
    Map getVersionLinks(String urlModelResourceType, SubscribedModel subscribedModel) {
        subscribedCatalogueService.
            getVersionLinksForModel(subscribedModel.subscribedCatalogue, urlModelResourceType, subscribedModel.subscribedModelId)
    }

    Map getNewerPublishedVersions(SubscribedModel subscribedModel) {
        subscribedCatalogueService.getNewerPublishedVersionsForPublishedModel(subscribedModel.subscribedCatalogue, subscribedModel.subscribedModelId)
    }

    /**
     * Export a model as bytes from a remote link from a SubscribedCatalogue
     *
     * @param subscribedModel , link
     * @return Byte array of the linked resource
     */
    byte[] exportSubscribedModelFromSubscribedCatalogue(SubscribedModel subscribedModel, Link link) {
        subscribedCatalogueService.getBytesResourceExport(subscribedModel.subscribedCatalogue, link.href)
    }

    /**
     * Create version links of type NEW_MODEL_VERSION_OF
     * @return
     */
    void addVersionLinksToImportedModel(User currentUser, Map versionLinks, ModelService modelService, SubscribedModel subscribedModel) {
        log.debug('addVersionLinksToImportedModel')
        List matches = []
        if (versionLinks && versionLinks.items) {
            matches = versionLinks.items.findAll {
                it.sourceModel.id == subscribedModel.subscribedModelId && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            }
        }

        if (matches) {
            matches.each {vl ->
                log.debug('matched')
                //Get Subscribed models for the new (source) and old (target) versions of the model
                SubscribedModel sourceSubscribedModel = subscribedModel
                SubscribedModel targetSubscribedModel = findBySubscribedCatalogueIdAndSubscribedModelId(subscribedModel.subscribedCatalogueId, vl.targetModel.id)

                if (sourceSubscribedModel && targetSubscribedModel) {
                    Model localSourceModel
                    Model localTargetModel

                    //Get the local copies of the new (source) and old (target) of the models
                    localSourceModel = modelService.get(sourceSubscribedModel.localModelId)
                    localTargetModel = modelService.get(targetSubscribedModel.localModelId)

                    //Create a local version link between the local copies of the new (source) and old (target) models
                    if (localSourceModel && localTargetModel) {
                        //Do we alreday have a version link between these two model versions?
                        boolean exists = localSourceModel.versionLinks && localSourceModel.versionLinks.find {
                            it.model.id == localSourceModel.id && it.targetModel.id == localTargetModel.id && it.linkType ==
                            VersionLinkType.NEW_MODEL_VERSION_OF
                        }

                        if (!exists) {
                            log.debug('setModelIsNewBranch')
                            modelService.setModelIsNewBranchModelVersionOfModel(localSourceModel, localTargetModel, currentUser)
                        }
                    }
                }
            }
        }
        log.debug('exit addVersionLinksToImportedModel')
    }

    private List<Link> getExportLinksForSubscribedModel(SubscribedModel subscribedModel) {
        List<PublishedModel> sourcePublishedModels = subscribedCatalogueService.listPublishedModels(subscribedModel.subscribedCatalogue)
            .findAll {pm -> pm.modelId == subscribedModel.subscribedModelId}
            .sort {pm -> pm.lastUpdated}
        // Atom feeds may allow multiple versions of an entry with the same ID

        if (sourcePublishedModels) {
            sourcePublishedModels.last().links
        } else {
            null
        }
    }

    void anonymise(String createdBy) {
        SubscribedModel.findAllByCreatedBy(createdBy).each {subscribedModel ->
            subscribedModel.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS
            subscribedModel.save(validate: false)
        }
    }
}
