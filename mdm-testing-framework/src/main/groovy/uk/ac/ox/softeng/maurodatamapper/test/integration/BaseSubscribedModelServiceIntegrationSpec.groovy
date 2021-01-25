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
package uk.ac.ox.softeng.maurodatamapper.test.integration

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.federation.AvailableModel
import uk.ac.ox.softeng.maurodatamapper.core.federation.SubscribedCatalogue
import uk.ac.ox.softeng.maurodatamapper.core.federation.SubscribedModel
import uk.ac.ox.softeng.maurodatamapper.core.federation.SubscribedModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ModelImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.FileParameter
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.ModelImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
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

    AvailableModel availableModelVersion1
    AvailableModel availableModelVersion2

    SubscribedCatalogue subscribedCatalogue
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

        folder = new Folder(label: 'Federation Folder', createdByUser: admin)
        checkAndSave(folder)

        //Mock a subscription to a remote catalogue
        subscribedCatalogue = new SubscribedCatalogue(url: 'http://remote.example.com',
                                                      apiKey: UUID.randomUUID(),
                                                      label: 'Test Remote Catalogue',
                                                      createdByUser: admin)
        checkAndSave(subscribedCatalogue)                                                      


        //Note: ID is hardcoded because we are mocking an external input rather than a domain created locally.
        //Don't need to save AvailableModel
        availableModelVersion1 = new AvailableModel(id: Utils.toUuid("c8023de6-5329-4b8b-8a1b-27c2abeaffcd"),
                                                    label: 'Remote Model 1.0.0',
                                                    description: 'Remote Model Description',
                                                    modelType: getModelType(),
                                                    lastUpdated: OffsetDateTime.now())

        availableModelVersion2 = new AvailableModel(id: Utils.toUuid("d8023de6-5329-4b8b-8a1b-27c2abeaffcd"),
                                                    label: 'Remote Model 2.0.0',
                                                    description: 'Remote Model Description',
                                                    modelType: getModelType(),
                                                    lastUpdated: OffsetDateTime.now())                                                    

        subscribedModelVersion1 = new SubscribedModel(subscribedModelId: Utils.toUuid("c8023de6-5329-4b8b-8a1b-27c2abeaffcd"),
                                                      folderId: getFolder().id,
                                                      subscribedCatalogue: subscribedCatalogue,
                                                      createdByUser: admin,
                                                      subscribedModelType: getModelType())
        checkAndSave(subscribedModelVersion1)

        subscribedModelVersion2 = new SubscribedModel(subscribedModelId: Utils.toUuid("d8023de6-5329-4b8b-8a1b-27c2abeaffcd"),
                                                      folderId: getFolder().id,
                                                      subscribedCatalogue: subscribedCatalogue,
                                                      createdByUser: admin,
                                                      subscribedModelType: getModelType())
        checkAndSave(subscribedModelVersion2)        
    }




    void "Test version linking of federated model"() {
        
        given:
        setupData()
        
        when: 'imported versions 1 and 2 of a model'
        ModelImporterProviderService modelImporterProviderService = modelService.getJsonModelImporterProviderService()
        ModelImporterProviderServiceParameters parameters1 = modelImporterProviderService.createNewImporterProviderServiceParameters()
        parameters1.importFile = new FileParameter(fileContents: getRemoteModelVersion1Json().getBytes())
        parameters1.folderId = getFolder().id
        parameters1.finalised = true        
        K importedModelVersion1 = modelImporterProviderService.importModel(getAdmin(), parameters1)
        importedModelVersion1.folder = folder
        checkAndSave(importedModelVersion1)
        subscribedModelVersion1.lastRead = OffsetDateTime.now()
        subscribedModelVersion1.localModelId = importedModelVersion1.id
        checkAndSave(subscribedModelVersion1)
        ModelImporterProviderServiceParameters parameters2 = modelImporterProviderService.createNewImporterProviderServiceParameters()
        parameters2.importFile = new FileParameter(fileContents: getRemoteModelVersion2Json().getBytes())
        parameters2.folderId = getFolder().id
        parameters2.finalised = true        
        K importedModelVersion2 = modelImporterProviderService.importModel(getAdmin(), parameters2)
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
        importedModelVersion2.versionLinks[0].catalogueItem.id == importedModelVersion2.id

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
        importedModelVersion2.versionLinks[0].catalogueItem.id == importedModelVersion2.id
    }    
}
