/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponent
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class DataFlowSpec extends ModelItemSpec<DataFlow> implements DomainUnitTest<DataFlow> {

    DataModel source
    DataModel target
    DataClass sourceCl
    DataClass targetCl
    DataElement sourceEl
    DataElement targetEl

    def setup() {
        log.debug('Setting up DataFlowSpec unit')
        mockDomains(DataClassComponent, DataElementComponent, PrimitiveType, DataElement, DataModel, DataClass)

        PrimitiveType sourceDt = new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'sourceDt')
        PrimitiveType targetDt = new PrimitiveType(createdBy: StandardEmailAddress.UNIT_TEST, label: 'tagretDt')

        sourceEl = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'sourceEl', dataType: sourceDt)
        targetEl = new DataElement(createdBy: StandardEmailAddress.UNIT_TEST, label: 'targetEl', dataType: targetDt)

        sourceCl = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'sourceClass').addToDataElements(sourceEl)
        targetCl = new DataClass(createdBy: StandardEmailAddress.UNIT_TEST, label: 'targetClass').addToDataElements(targetEl)

        source = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'source', folder: testFolder, type: DataModelType.DATA_ASSET,
                               authority: testAuthority)
        target = new DataModel(createdBy: StandardEmailAddress.UNIT_TEST, label: 'target', folder: testFolder, type: DataModelType.DATA_ASSET,
                               authority: testAuthority)

        source.addToDataTypes(sourceDt)
        source.addToDataClasses(sourceCl)
        target.addToDataTypes(targetDt)
        target.addToDataClasses(targetCl)

        checkAndSave(source)
        checkAndSave(target)

        assert DataModel.count() == 2
        assert DataElement.count() == 2
        assert PrimitiveType.count() == 2
    }

    @Override
    void wipeModel() {
        domain.target = null
        domain.breadcrumbTree = null
    }

    @Override
    void setModel(DataFlow domain, Model model) {
        domain.target = model as DataModel
    }

    @Override
    void setValidDomainOtherValues() {
        domain.source = source
        domain.target = target
        domain
    }

    @Override
    void verifyDomainOtherConstraints(DataFlow subDomain) {
        assert subDomain.target.id == target.id
        assert subDomain.source.id == source.id
    }

    @Override
    DataFlow createValidDomain(String label) {
        new DataFlow(source: source, target: target, label: label, createdBy: StandardEmailAddress.UNIT_TEST)
    }

    @Override
    Model getOwningModel() {
        target
    }

    @Override
    String getModelFieldName() {
        'target'
    }
}
