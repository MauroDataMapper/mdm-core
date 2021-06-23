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
package uk.ac.ox.softeng.maurodatamapper.datamodel

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClassService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElementService

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class BootStrap {

    @Autowired
    MessageSource messageSource

    AuthorityService authorityService

    DataModelService dataModelService

    DataElementService dataElementService

    DataClassService dataClassService

    MetadataService metadataService

    AssetResourceLocator assetResourceLocator

    def init = { servletContext ->

        log.debug('Main bootstrap complete')

        environments {
            development {
                Folder.withNewTransaction {
                    Folder folder = Folder.findByLabel('Development Folder')
                    Authority authority = authorityService.getDefaultAuthority()
                    if (DataModel.countByLabel(BootstrapModels.COMPLEX_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, authority)
                    }
                    if (DataModel.countByLabel(BootstrapModels.SIMPLE_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder, authority)
                    }
                    if (DataModel.countByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveFinalisedSimpleDataModel(messageSource, folder, authority)
                    }
                    if (DataModel.countByLabel(BootstrapModels.MODEL_VERSION_TREE_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveModelVersionTree(messageSource, folder, authority, dataModelService, dataClassService,
                                                                     dataElementService, metadataService, assetResourceLocator)
                    }
                    if (DataModel.countByAuthorityIsNull() != 0) {
                        log.warn('DataModels missing authority, updating with default authority')
                        DataModel.findAllByAuthorityIsNull([fetch: [authority: 'lazy']]).each {
                            it.authority = authority
                            log.debug('Saving {}', it.label)
                            it.save(validate: false, flush: true)
                        }
                    }
                }
                log.debug('Development environment bootstrap complete')
            }
        }
    }
    def destroy = {
    }
}
