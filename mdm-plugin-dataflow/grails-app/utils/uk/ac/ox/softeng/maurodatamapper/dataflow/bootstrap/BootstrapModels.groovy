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
package uk.ac.ox.softeng.maurodatamapper.dataflow.bootstrap

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataElementComponent
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType

import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

class BootstrapModels {

    public static final String SOURCE_DATAMODEL_NAME = 'SourceFlowDataModel'
    public static final String TARGET_DATAMODEL_NAME = 'TargetFlowDataModel'
    public static final String DATAFLOW_NAME = 'Sample DataFlow'

    static DataModel buildAndSaveSourceDataModel(MessageSource messageSource, Folder folder, Authority authority) {

        DataModel dataModel = new DataModel(createdBy: DEVELOPMENT, label: SOURCE_DATAMODEL_NAME, folder: folder, type: DataModelType.DATA_ASSET,
                                            authority: authority)
            checkAndSave(messageSource, dataModel)

            PrimitiveType string = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')
            PrimitiveType integer = new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer')

            DataClass tableA = new DataClass(label: 'tableA', createdBy: DEVELOPMENT)
                .addToDataElements(label: 'columnA', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnB', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnC', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnD', createdBy: DEVELOPMENT, dataType: integer)

            DataClass tableB = new DataClass(label: 'tableB', createdBy: DEVELOPMENT)
                .addToDataElements(label: 'columnE1', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnF', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnG', createdBy: DEVELOPMENT, dataType: integer)
                .addToDataElements(label: 'columnH', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnI', createdBy: DEVELOPMENT, dataType: integer)

            DataClass tableC = new DataClass(label: 'tableC', createdBy: DEVELOPMENT)
                .addToDataElements(label: 'columnE2', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnJ', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnK', createdBy: DEVELOPMENT, dataType: integer)
                .addToDataElements(label: 'columnL', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnM', createdBy: DEVELOPMENT, dataType: integer)

            dataModel
                .addToDataTypes(string)
                .addToDataTypes(integer)
                .addToDataClasses(tableA)
                .addToDataClasses(tableB)
                .addToDataClasses(tableC)

            checkAndSave(messageSource, dataModel)
        dataModel
    }

    static DataModel buildAndSaveTargetDataModel(MessageSource messageSource, Folder folder, Authority authority) {

        DataModel dataModel = new DataModel(createdBy: DEVELOPMENT, label: TARGET_DATAMODEL_NAME, folder: folder, type: DataModelType.DATA_ASSET,
                                            authority: authority)
            checkAndSave(messageSource, dataModel)

            PrimitiveType string = new PrimitiveType(createdBy: DEVELOPMENT, label: 'string')
            PrimitiveType integer = new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer')

            DataClass tableD = new DataClass(label: 'tableD', createdBy: DEVELOPMENT)
                .addToDataElements(label: 'columnN', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnO', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnP', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnQ', createdBy: DEVELOPMENT, dataType: integer)

            DataClass tableE = new DataClass(label: 'tableE', createdBy: DEVELOPMENT)
                .addToDataElements(label: 'columnE', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnR', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnS', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnT', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnU', createdBy: DEVELOPMENT, dataType: string)
                .addToDataElements(label: 'columnV', createdBy: DEVELOPMENT, dataType: string)

            dataModel
                .addToDataTypes(string)
                .addToDataTypes(integer)
                .addToDataClasses(tableD)
                .addToDataClasses(tableE)

            checkAndSave(messageSource, dataModel)
        dataModel
    }

    static DataFlow buildAndSaveSampleDataFlow(MessageSource messageSource) {
        DataModel sourceDataModel = DataModel.findByLabel(SOURCE_DATAMODEL_NAME)
        DataModel targetDataModel = DataModel.findByLabel(TARGET_DATAMODEL_NAME)

        Map<String, DataClass> sourceDataClasses = sourceDataModel.dataClasses.collectEntries {
            [it.label, it]
        }
        Map<String, DataClass> targetDataClasses = targetDataModel.dataClasses.collectEntries {
            [it.label, it]
        }
        Map<String, DataElement> sourceDataElements = sourceDataClasses.values().collectMany {it.dataElements}.collectEntries {
            [it.label, it]
        }

        Map<String, DataElement> targetDataElements = targetDataClasses.values().collectMany {it.dataElements}.collectEntries {
            [it.label, it]
        }

        DataFlow dataFlow = new DataFlow(label: DATAFLOW_NAME, createdBy: DEVELOPMENT, source: sourceDataModel, target: targetDataModel)
            checkAndSave(messageSource, dataFlow)

            dataFlow.addToDataClassComponents(
                new DataClassComponent(label: 'aToD', createdBy: DEVELOPMENT,
                                       definition: '''SELECT *
INTO TargetFlowDataModel.tableD
FROM SourceFlowDataModel.tableA''')
                    .addToSourceDataClasses(sourceDataClasses.tableA)
                    .addToTargetDataClasses(targetDataClasses.tableD)
                    .addToDataElementComponents(
                        new DataElementComponent(label: 'Direct Copy', createdBy: DEVELOPMENT)
                            .addToSourceDataElements(sourceDataElements.columnA)
                            .addToTargetDataElements(targetDataElements.columnN)
                    )
                    .addToDataElementComponents(
                        new DataElementComponent(label: 'Direct Copy', createdBy: DEVELOPMENT)
                            .addToSourceDataElements(sourceDataElements.columnB)
                            .addToTargetDataElements(targetDataElements.columnO)
                    )
                    .addToDataElementComponents(
                        new DataElementComponent(label: 'Direct Copy', createdBy: DEVELOPMENT)
                            .addToSourceDataElements(sourceDataElements.columnC)
                            .addToTargetDataElements(targetDataElements.columnP)
                    )
                    .addToDataElementComponents(
                        new DataElementComponent(label: 'Direct Copy', createdBy: DEVELOPMENT)
                            .addToSourceDataElements(sourceDataElements.columnD)
                            .addToTargetDataElements(targetDataElements.columnQ)
                    )
            )
                .addToDataClassComponents(
                    new DataClassComponent(label: 'bAndCToE', createdBy: DEVELOPMENT,
                                           definition: '''INSERT INTO TargetFlowDataModel.tableE
SELECT
    b.columnE1                                      AS columnE,
    b.columnF                                       AS columnR,
    CONCAT(b.columnG,'_',c.columnJ)                 AS columnS,
    CASE
        WHEN b.columnH IS NULL THEN b.columnI
        ELSE b.columnH
    END                                             AS columnT,
    TRIM(c.columnJ)                                 AS columnU,
    CONCAT(c.columnL,' ',c.columnM,'--',b.columnG)  AS columnV
FROM SourceFlowDataModel.tableB b
INNER JOIN SourceFlowDataModel.tableC c ON b.columnE1 = c.columnE2''')
                        .addToSourceDataClasses(sourceDataClasses.tableB)
                        .addToSourceDataClasses(sourceDataClasses.tableC)
                        .addToTargetDataClasses(targetDataClasses.tableE)
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'JOIN KEY', createdBy: DEVELOPMENT)
                                .addToSourceDataElements(sourceDataElements.columnE1)
                                .addToSourceDataElements(sourceDataElements.columnE2)
                                .addToTargetDataElements(targetDataElements.columnE)
                        )
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'Direct Copy', createdBy: DEVELOPMENT)
                                .addToSourceDataElements(sourceDataElements.columnF)
                                .addToTargetDataElements(targetDataElements.columnR)
                        )
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'CONCAT', createdBy: DEVELOPMENT, definition: 'CONCAT(b.columnG,\'_\',c.columnJ)')
                                .addToSourceDataElements(sourceDataElements.columnG)
                                .addToSourceDataElements(sourceDataElements.columnJ)
                                .addToTargetDataElements(targetDataElements.columnS)
                        )
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'CASE', createdBy: DEVELOPMENT, definition: '''
CASE
    WHEN b.columnH IS NULL THEN b.columnI
    ELSE b.columnH
END''')
                                .addToSourceDataElements(sourceDataElements.columnH)
                                .addToSourceDataElements(sourceDataElements.columnI)
                                .addToTargetDataElements(targetDataElements.columnT)
                        )
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'TRIM', createdBy: DEVELOPMENT)
                                .addToSourceDataElements(sourceDataElements.columnJ)
                                .addToTargetDataElements(targetDataElements.columnU)
                        )
                        .addToDataElementComponents(
                            new DataElementComponent(label: 'CONCAT', createdBy: DEVELOPMENT,
                                                     definition: 'CONCAT(c.columnL,\' \',c.columnM,\'--\',b.columnG)')
                                .addToSourceDataElements(sourceDataElements.columnL)
                                .addToSourceDataElements(sourceDataElements.columnM)
                                .addToSourceDataElements(sourceDataElements.columnG)
                                .addToTargetDataElements(targetDataElements.columnV)
                        )
                )

            checkAndSave(messageSource, dataFlow)
        dataFlow
    }

}
