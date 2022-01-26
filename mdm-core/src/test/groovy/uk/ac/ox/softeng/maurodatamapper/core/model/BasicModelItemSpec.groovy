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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

import java.time.OffsetDateTime

/**
 * @since 17/02/2020
 */
@Slf4j
class BasicModelItemSpec extends ModelItemSpec<BasicModelItem> implements DomainUnitTest<BasicModelItem> {

    def setup() {
        log.debug('Setting up BasicModelItem unit')
        mockDomains(BasicModel, Authority)

        checkAndSave(new BasicModel(label: 'test model', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority))
    }

    @Override
    void wipeModel() {
        domain.model = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(BasicModelItem domain, Model model) {
        domain.model = model as BasicModel
    }

    @Override
    void setValidDomainOtherValues() {
        domain.dateCreated = OffsetDateTime.now()
        domain.lastUpdated = OffsetDateTime.now()
    }

    @Override
    void verifyDomainOtherConstraints(BasicModelItem domain) {
    }

    @Override
    BasicModelItem createValidDomain(String label) {
        new BasicModelItem(label: label, model: BasicModel.findByLabel('test model'), createdBy: editor.emailAddress)
    }

    @Override
    Model getOwningModel() {
        BasicModel.findByLabel('test model')
    }

    @Override
    String getModelFieldName() {
        'model'
    }
}
