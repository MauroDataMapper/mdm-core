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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

class BootStrap {
    @Autowired
    MessageSource messageSource
    @Autowired
    TerminologyService terminologyService

    def init = {servletContext ->
        environments {
            development {
                Folder.withNewTransaction {
                    Folder folder = Folder.findByLabel('Development Folder')
                    if (Terminology.countByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME) == 0) {
                        BootstrapModels.buildAndSaveComplexTerminology(messageSource, folder, terminologyService)
                    }
                    if (Terminology.countByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME) == 0) {
                        BootstrapModels.buildAndSaveSimpleTerminology(messageSource, folder)
                    }
                    if (CodeSet.countByLabel(BootstrapModels.SIMPLE_CODESET_NAME) == 0) {
                        BootstrapModels.buildAndSaveSimpleCodeSet(messageSource, folder)
                    }
                }
            }
        }
    }
    def destroy = {
    }
}