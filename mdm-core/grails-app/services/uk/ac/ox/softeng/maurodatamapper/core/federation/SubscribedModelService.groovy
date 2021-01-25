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
package uk.ac.ox.softeng.maurodatamapper.core.federation

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.transactions.Transactional

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import io.micronaut.http.HttpResponse
import static io.micronaut.http.HttpStatus.OK

import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Transactional
class SubscribedModelService {

    @Autowired(required = false)
    List<ModelService> modelServices

    SubscribedCatalogueService subscribedCatalogueService

    SubscribedModel get(Serializable id) {
        SubscribedModel.get(id)
    }

    SubscribedModel findBySubscribedCatalogueIdAndSubscribedModelId(UUID subscribedCatalogueId, UUID subscribedModelId) {
        SubscribedModel.bySubscribedCatalogueIdAndSubscribedModelId(subscribedCatalogueId, subscribedModelId).get()
    }

    SubscribedModel findBySubscribedModelId(UUID subscribedModelId) {
        SubscribedModel.bySubscribedModelId(subscribedModelId).get()
    }    

    List<SubscribedModel> list(Map pagination) {
        pagination ? SubscribedModel.list(pagination) : SubscribedModel.list()
    }

    Long count() {
        SubscribedModel.count()
    }

    void delete(SubscribedModel subscribedModel) {
        subscribedModel.delete(flush: true)
    }

    SubscribedModel save(SubscribedModel subscribedModel) {
        subscribedModel.save(failOnError: true, validate: false)
    }

    /**
     * Find an exporter for the domain type that we want to export from the subscribed catalogue
     * 1. Create a URL for {modelDomainType}/providers/exporters
     * 2. Slurp the Json returned by this endpoint
     * 3. Return an exporter from this slurped json which has a file extension of json
     *
     * @param subscribedCatalogue
     * @param prefix
     * @return An exporter
     */ 
    private SubscribedCatalogueExporter getExporter(SubscribedCatalogue subscribedCatalogue, String prefix) {
        SubscribedCatalogueExporter exporter

        String endpoint = makeExporterProviderUrl(subscribedCatalogue.url, prefix)
        HttpResponse response = subscribedCatalogueService.GET(endpoint, subscribedCatalogue.apiKey)

        if (response.status() == OK) {
            JsonSlurper slurper = new JsonSlurper()

            //Make an object by slurping json
            def exporters = slurper.parseText(response.body())

            //Find a json exporter
            def object = exporters.find {it.fileExtension == 'json'}

            //Can't use DataBindingUtils because of a clash with grails 'version' property
            if (object && object.namespace && object.name && object.version) {
                exporter = new SubscribedCatalogueExporter(object.namespace, object.name, object.version)
            }
        }

        exporter
    }


    /**
     * Get version links from the subscribed catalogue for the specified subscribed model
     * 1. Create a URL for {modelDomainType}/{modelId}/versionLinks
     * 2. Slurp the Json returned by this endpoint and return the object
     *
     * @param prefix
     * @param subscribedModel
     * @return A map of slurped version links
     */
    Map getVersionLinks(String prefix, SubscribedModel subscribedModel) {
        Map versionLinks

        String endpoint = makeVersionLinkUrl(subscribedModel, prefix)

        HttpResponse response = subscribedCatalogueService.GET(endpoint, subscribedModel.subscribedCatalogue.apiKey)

        log.debug("Response status {}", response.status())
            
        if (response.status() == OK) {
            JsonSlurper slurper = new JsonSlurper()

            //Make an object by slurping json
            versionLinks = slurper.parseText(response.body())
        }

        versionLinks
    }

    /**
     * Export a model as Json from a remote SubscribedCatalogue
     * 1. Find an exporter on the remote SubscribedCatalogue
     * 2. If an exporter is found, use it to export the model
     *
     * @param subscribedModel
     * @return String of the exported json
     */
    String exportSubscribedModelFromSubscribedCatalogue(SubscribedModel subscribedModel) {        
        String json
        
        //Get a ModelService to handle the domain type we are dealing with
        ModelService modelService = modelServices.find {it.handles(subscribedModel.subscribedModelType)}

        def exporter = getExporter(subscribedModel.subscribedCatalogue, modelService.getPrefix())

        if (exporter) {
            log.debug("Create an export URL")
            String endpoint = String.format("%s/%s/%s/%s/export/%s/%s/%s",
                                            subscribedModel.subscribedCatalogue.url,
                                            SubscribedCatalogueService.BASE_PATH,
                                            modelService.getPrefix(),
                                            subscribedModel.subscribedModelId,
                                            exporter.exporterNamespace,
                                            exporter.exporterName,
                                            exporter.exporterVersion)

            HttpResponse response = subscribedCatalogueService.GET(endpoint, subscribedModel.subscribedCatalogue.apiKey)

            log.debug("Response status {}", response.status())
            
            if (response.status() == OK) {
                json = response.body()
            }
        }

        json
    }

    /**
     * Create version links of type NEW_MODEL_VERSION_OF
     * @return
     */
    void addVersionLinksToImportedModel(User currentUser, Map versionLinks, ModelService modelService, SubscribedModel subscribedModel) {
        log.debug("addVersionLinksToImportedModel") 
        List matches = []
        if (versionLinks && versionLinks.items) {
            matches = versionLinks.items.findAll {
                it.sourceModel.id == subscribedModel.subscribedModelId.toString() && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF.label
            }
        }
        
        if (matches) {
            matches.each {vl ->
                log.debug("matched")
                //Get Subscribed models for the new (source) and old (target) versions of the model
                SubscribedModel sourceSubscribedModel = findBySubscribedModelId(UUID.fromString(vl.sourceModel.id))
                SubscribedModel targetSubscribedModel = findBySubscribedModelId(UUID.fromString(vl.targetModel.id))

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
                            it.catalogueItem.id == localSourceModel.id && it.targetModel.id == localTargetModel.id && it.linkType == VersionLinkType.NEW_MODEL_VERSION_OF
                        }

                        if (!exists) {
                            log.debug("setModelIsNewBranch")
                            modelService.setModelIsNewBranchModelVersionOfModel(localSourceModel, localTargetModel, currentUser)
                        }                        
                    }
                }
            }
        }
        log.debug("exit addVersionLinksToImportedModel") 
    }    


    /**
     * Make a URL for an endpoint on the subscribed catalogue which will return a list of exporters for the specified
     * domain type
     *
     * @param url The root URL of the subscribed catalogue
     * @param prefix The model prefix
     * @return a URL for the /{domainType}/providers/exporters endpoint
     */
    private static String makeExporterProviderUrl(String url, String prefix) {
        return String.format("%s/%s/%s/providers/exporters", url, SubscribedCatalogueService.BASE_PATH, prefix)
    }

    /**
     * Make a URL for an endpoint on the subscribed catalogue which will return a list of version links
     *
     * @param subscribedModel The SubscribedModel we want version links for
     * @param prefix The model prefix
     * @return a URL for the /{domainType}/providers/exporters endpoint
     */
    private static String makeVersionLinkUrl(SubscribedModel subscribedModel, String prefix) {
        return String.format("%s/%s/%s/%s/versionLinks",
                             subscribedModel.subscribedCatalogue.url,
                             SubscribedCatalogueService.BASE_PATH,
                             prefix,
                             subscribedModel.subscribedModelId)
    }      
}
