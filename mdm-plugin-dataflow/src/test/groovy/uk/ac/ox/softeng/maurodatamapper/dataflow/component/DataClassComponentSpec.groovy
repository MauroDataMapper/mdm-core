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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelItemSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class DataClassComponentSpec extends ModelItemSpec<DataClassComponent> implements DomainUnitTest<DataClassComponent> {

    DataModel source
    DataModel target
    DataClass sourceCl
    DataClass targetCl
    DataElement sourceEl
    DataElement targetEl
    DataFlow dataFlow

    def setup() {

        log.debug('Setting up DataFlowSpec unit')
        mockDomains(DataFlow, DataClassComponent, DataElementComponent, PrimitiveType, DataElement, DataModel, DataClass)

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

        dataFlow = new DataFlow(createdBy: StandardEmailAddress.UNIT_TEST, label: 'dataflow', source: source, target: target)
        checkAndSave(dataFlow)

        assert DataElement.count() == 2
        assert PrimitiveType.count() == 2
    }

    @Override
    int getExpectedBaseConstrainedErrorCount() {
        1
    }

    @Override
    void wipeModel() {
        domain.breadcrumbTree = null
        domain.dataFlow = null
    }

    @Override
    void setModel(DataClassComponent domain, Model model) {
        domain.dataFlow = dataFlow
    }

    @Override
    void setValidDomainOtherValues() {
        domain.dataFlow = dataFlow
        domain.addToTargetDataClasses(targetCl)
        domain.addToSourceDataClasses(sourceCl)
        domain
    }

    @Override
    void verifyDomainOtherConstraints(DataClassComponent domain) {
        assert domain.dataFlow.id == dataFlow.id
        assert domain.sourceDataClasses.size() == 1
        assert domain.targetDataClasses.size() == 1
        assert domain.sourceDataClasses.find {it.id == sourceCl.id}
        assert domain.targetDataClasses.find {it.id == targetCl.id}
    }

    @Override
    DataClassComponent createValidDomain(String label) {
        DataClassComponent other = new DataClassComponent(label: label, createdBy: StandardEmailAddress.UNIT_TEST)
        other.addToTargetDataClasses(targetCl)
        other.addToSourceDataClasses(sourceCl)
        dataFlow.addToDataClassComponents(other)
        other
    }

    @Override
    Model getOwningModel() {
        target
    }

    @Override
    String getModelFieldName() {
        null
    }
}
