/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import org.springframework.context.MessageSource

class BootStrap {

    MessageSource messageSource

    def init = {servletContext ->

        environments {
            development {
                Folder.withNewTransaction {
                    Folder folder = Folder.findByLabel('Development Folder')
                    if (DataModel.countByLabel(BootstrapModels.SOURCE_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveSourceDataModel(messageSource, folder)
                    }
                    if (DataModel.countByLabel(BootstrapModels.TARGET_DATAMODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveTargetDataModel(messageSource, folder)
                    }
                    if (DataFlow.countByLabel(BootstrapModels.DATAFLOW_NAME) == 0) {
                        BootstrapModels.buildAndSaveSampleDataFlow(messageSource)
                    }
                }
            }
        }

    }
    def destroy = {
    }
}
