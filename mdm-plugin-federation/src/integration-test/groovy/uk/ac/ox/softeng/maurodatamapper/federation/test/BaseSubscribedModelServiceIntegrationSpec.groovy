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
package uk.ac.ox.softeng.maurodatamapper.federation.test

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.federation.PublishedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedCatalogueService
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModel
import uk.ac.ox.softeng.maurodatamapper.federation.SubscribedModelService
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.nio.charset.Charset
import java.time.OffsetDateTime

@Slf4j
@Integration
@Rollback
abstract class BaseSubscribedModelServiceIntegrationSpec<K extends Model> extends BaseIntegrationSpec {

    PublishedModel availableModelVersion1
    PublishedModel availableModelVersion2

    SubscribedCatalogue subscribedCatalogue
    SubscribedCatalogue subscribedCatalogue2
    SubscribedCatalogueService subscribedCatalogueService
    SubscribedModelService subscribedModelService

    SubscribedModel subscribedModelVersion1
    SubscribedModel subscribedModelVersion2

    abstract ModelService getModelService()

    abstract String getModelType()

    abstract String getRemoteModelVersion1Json()

    abstract String getRemoteModelVersion2Json()

    abstract String getRemoteModelVersion2VersionLinks()

    @Override
    void setupDomainData() {
        log.debug('Setting up BaseSubscribedCatalogueServiceIntegrationSpec')

        folder = new Folder(label: 'Federation Folder', createdBy: StandardEmailAddress.INTEGRATION_TEST)
        checkAndSave(folder)

        //Mock a subscription to a remote catalogue
        subscribedCatalogue = new SubscribedCatalogue(url: 'http://remote.example.com',
                                                      apiKey: UUID.randomUUID(),
                                                      label: 'Test Remote Catalogue',
                                                      createdBy: StandardEmailAddress.ADMIN)
        checkAndSave(subscribedCatalogue)

        subscribedCatalogue2 = new SubscribedCatalogue(url: 'http://remote2.example.com',
                                                       apiKey: UUID.randomUUID(),
                                                       label: 'Test Remote Catalogue 2',
                                                       createdBy: editor.emailAddress)
        checkAndSave(subscribedCatalogue2)


        //Note: ID is hardcoded because we are mocking an external input rather than a domain created locally.
        //Don't need to save AvailableModel
        availableModelVersion1 = new PublishedModel(modelId: Utils.toUuid('c8023de6-5329-4b8b-8a1b-27c2abeaffcd'),
                                                    title: 'Remote Model 1.0.0',
                                                    description: 'Remote Model Description',
                                                    modelType: getModelType(),
                                                    lastUpdated: OffsetDateTime.now())

        availableModelVersion2 = new PublishedModel(modelId: Utils.toUuid('d8023de6-5329-4b8b-8a1b-27c2abeaffcd'),
                                                    title: 'Remote Model 2.0.0',
                                                    description: 'Remote Model Description',
                                                    modelType: getModelType(),
                                                    lastUpdated: OffsetDateTime.now())

        subscribedModelVersion1 = new SubscribedModel(subscribedModelId: Utils.toUuid('c8023de6-5329-4b8b-8a1b-27c2abeaffcd'),
                                                      folderId: getFolder().id,
                                                      subscribedCatalogue: subscribedCatalogue,
                                                      createdBy: StandardEmailAddress.ADMIN,
                                                      subscribedModelType: getModelType())
        checkAndSave(subscribedModelVersion1)

        subscribedModelVersion2 = new SubscribedModel(subscribedModelId: Utils.toUuid('d8023de6-5329-4b8b-8a1b-27c2abeaffcd'),
                                                      folderId: getFolder().id,
                                                      subscribedCatalogue: subscribedCatalogue,
                                                      createdBy: editor.emailAddress,
                                                      subscribedModelType: getModelType())
        checkAndSave(subscribedModelVersion2)
    }


    void 'Test version linking of federated model'() {

        given:
        setupData()

        when: 'imported versions 1 a model'
        ModelImporterProviderService modelImporterProviderService = modelService.getJsonModelImporterProviderService()
        ModelImporterProviderServiceParameters parameters1 = (modelImporterProviderService.createNewImporterProviderServiceParameters() as
            ModelImporterProviderServiceParameters).tap {
            importFile = new FileParameter(fileContents: getRemoteModelVersion1Json().getBytes())
            folderId = getFolder().id
            finalised = true
        }
        K importedModelVersion1 = modelImporterProviderService.importDomain(getAdmin(), parameters1) as K
        importedModelVersion1.folder = folder
        checkAndSave(importedModelVersion1)

        subscribedModelVersion1.lastRead = OffsetDateTime.now()
        subscribedModelVersion1.localModelId = importedModelVersion1.id
        checkAndSave(subscribedModelVersion1)

        and: 'import version 2 of a model'
        ModelImporterProviderServiceParameters parameters2 = (modelImporterProviderService.createNewImporterProviderServiceParameters() as
            ModelImporterProviderServiceParameters).tap {
            importFile = new FileParameter(fileContents: getRemoteModelVersion2Json().getBytes())
            folderId = getFolder().id
            finalised = true
        }
        K importedModelVersion2 = modelImporterProviderService.importDomain(getAdmin(), parameters2) as K
        importedModelVersion2.folder = folder
        checkAndSave(importedModelVersion2)

        subscribedModelVersion2.lastRead = OffsetDateTime.now()
        subscribedModelVersion2.localModelId = importedModelVersion2.id
        checkAndSave(subscribedModelVersion2)

        then: 'there are no version links'
        !modelService.get(importedModelVersion1.id).versionLinks
        !importedModelVersion2.versionLinks

        when: 'we try to create the version link from a json response'
        JsonSlurper slurper = new JsonSlurper()
        subscribedModelService.addVersionLinksToImportedModel(
            getAdmin(),
            slurper.parseText(new String(getRemoteModelVersion2VersionLinks().getBytes(), Charset.defaultCharset())),
            modelService,
            subscribedModelVersion2
        )
        checkAndSave(importedModelVersion1)
        checkAndSave(importedModelVersion2)

        then: 'the version link is created between model version 1 and model version 2'
        !modelService.get(importedModelVersion1.id).versionLinks
        importedModelVersion2.versionLinks.size() == 1
        importedModelVersion2.versionLinks[0].targetModel.id == importedModelVersion1.id
        importedModelVersion2.versionLinks[0].model.id == importedModelVersion2.id

        when: 'we try to create the same version link again from a json response'
        subscribedModelService.addVersionLinksToImportedModel(
            getAdmin(),
            slurper.parseText(new String(getRemoteModelVersion2VersionLinks().getBytes(), Charset.defaultCharset())),
            modelService,
            subscribedModelVersion2
        )
        checkAndSave(importedModelVersion1)
        checkAndSave(importedModelVersion2)

        then: 'we have just the same version link as before'
        !modelService.get(importedModelVersion1.id).versionLinks
        importedModelVersion2.versionLinks.size() == 1
        importedModelVersion2.versionLinks[0].targetModel.id == importedModelVersion1.id
        importedModelVersion2.versionLinks[0].model.id == importedModelVersion2.id
    }

    void 'test anonymisation of subscribed catalogues and models'() {
        given:
        setupData()

        when: 'list all subscribed catalogues and models'
        List<SubscribedCatalogue> subscribedCatalogues = subscribedCatalogueService.list()
        List<SubscribedModel> subscribedModels = subscribedModelService.list()

        then: 'there are two of each, in each case 1 created by admin, 1 created by editor and none by anonymous'
        subscribedModels.findAll{it.createdBy == admin.emailAddress}.size() == 1
        subscribedModels.findAll{it.createdBy == editor.emailAddress}.size() == 1
        subscribedModels.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 0
        subscribedCatalogues.findAll{it.createdBy == admin.emailAddress}.size() == 1
        subscribedCatalogues.findAll{it.createdBy == editor.emailAddress}.size() == 1
        subscribedCatalogues.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 0

        when: 'anonymise editor on catalogues and models'
        subscribedCatalogueService.anonymise(editor.emailAddress)
        subscribedModelService.anonymise(editor.emailAddress)
        subscribedCatalogues = subscribedCatalogueService.list()
        subscribedModels = subscribedModelService.list()

        then: 'the catalogue and models that were created by editor are now created by anonymous'
        subscribedModels.findAll{it.createdBy == admin.emailAddress}.size() == 1
        subscribedModels.findAll{it.createdBy == editor.emailAddress}.size() == 0
        subscribedModels.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 1
        subscribedCatalogues.findAll{it.createdBy == admin.emailAddress}.size() == 1
        subscribedCatalogues.findAll{it.createdBy == editor.emailAddress}.size() == 0
        subscribedCatalogues.findAll{it.createdBy == 'anonymous@maurodatamapper.com'}.size() == 1
    }
}
