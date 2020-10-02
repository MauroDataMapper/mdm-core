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
package uk.ac.ox.softeng.maurodatamapper.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class BootStrap {

    @Autowired
    MessageSource messageSource

    AuthorityService authorityService

    def init = { servletContext ->

        log.debug('Main bootstrap complete')

        environments {
            development {
                Folder.withNewTransaction {
                    Folder folder = Folder.findByLabel('Development Folder')
                    Authority authority = authorityService.getDefaultAuthority()
                    if (ReferenceDataModel.countByLabel(BootstrapModels.SIMPLE_REFERENCE_MODEL_NAME) == 0) {
                        BootstrapModels.buildAndSaveExampleReferenceDataModel(messageSource, folder, authority)
                    }
                    if (ReferenceDataModel.countByAuthorityIsNull() != 0) {
                        log.warn('ReferenceDataModels missing authority, updating with default authority')
                        ReferenceDataModel.findAllByAuthorityIsNull().each {
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
