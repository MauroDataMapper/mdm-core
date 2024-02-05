/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.authority.AuthorityService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class BootStrap {
    @Autowired
    MessageSource messageSource
    @Autowired
    TerminologyService terminologyService

    AuthorityService authorityService

    def init = { servletContext ->

        log.debug('Main bootstrap complete')

        environments {
            development {
                Folder.withNewTransaction {
                    Folder folder = Folder.findByLabel('Development Folder')
                    Authority authority = authorityService.getDefaultAuthority()
                    if (Terminology.countByLabel(BootstrapModels.SIMPLE_TERMINOLOGY_NAME) == 0) {
                        BootstrapModels.buildAndSaveSimpleTerminology(messageSource, folder, authority)
                    }
                    if (Terminology.countByLabel(BootstrapModels.COMPLEX_TERMINOLOGY_NAME) == 0) {
                        BootstrapModels.buildAndSaveComplexTerminology(messageSource, folder, terminologyService, authority)
                    }
                    if (CodeSet.countByLabel(BootstrapModels.SIMPLE_CODESET_NAME) == 0) {
                        BootstrapModels.buildAndSaveSimpleCodeSet(messageSource, folder, authority)
                    }
                    if (CodeSet.countByLabel(BootstrapModels.COMPLEX_CODESET_NAME) == 0) {
                        BootstrapModels.buildAndSaveComplexCodeSet(messageSource, folder, terminologyService, authority)
                    }
                    if (CodeSet.countByLabel(BootstrapModels.UNFINALISED_CODESET_NAME) == 0) {
                        BootstrapModels.buildAndSaveUnfinalisedCodeSet(messageSource, folder, authority)
                    }

                    if (Terminology.countByAuthorityIsNull() != 0) {
                        log.warn('Terminologies missing authority, updating with default authority')
                        Terminology.findAllByAuthorityIsNull([fetch: [authority: 'lazy']]).each {
                            it.authority = authority
                            log.debug('Saving {}', it.label)
                            it.save(validate: false, flush: true)
                        }
                    }
                    if (CodeSet.countByAuthorityIsNull() != 0) {
                        log.warn('CodeSets missing authority, updating with default authority')
                        CodeSet.findAllByAuthorityIsNull([fetch: [authority: 'lazy']]).each {
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
