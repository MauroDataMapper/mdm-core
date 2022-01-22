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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelImportFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Ignore

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: dataType
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes/${otherDataModelId}/${dataTypeId}  | Action: copyDataType
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeController
 */
@Integration
@Slf4j
@Ignore('No longer relevant')
class DataElementModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getIndexPath() {
        "dataModels/${getImportingDataModelId()}/dataClasses/${getCatalogueItemId()}/dataElements"
    }

    @Override
    String getModelImportPath() {
        "dataClasses/${getCatalogueItemId()}/modelImports"
    }

    @Override
    List getAdditionalModelImportPaths() {
        [getDataTypeModelImportPath()]
    }

    String getDataTypeResourcePath() {
        "dataModels/${getImportingDataModelId()}/dataTypes"
    }

    String getDataTypeModelImportPath() {
        "dataModels/${getImportingDataModelId()}/modelImports"
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        String dataClassId = DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id,
                                                             BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
        DataElement.byDataClassIdAndLabel(dataClassId, 'data element 1').get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        DataElement.simpleName
    }

    @Transactional
    @Override
    String getCatalogueItemId() {
        DataClass.byDataModelIdAndLabel(Utils.toUuid(importingDataModelId), 'importing class 3').get().id.toString()
    }

    @Override
    String getCatalogueItemDomainType() {
        'DataClass'
    }

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_3).id.toString()
    }

    @Transactional
    String getImportedStringDataTypeId() {
        PrimitiveType.byDataModelIdAndLabel(Utils.toUuid(getFinalisedSimpleDataModelId()), 'gnirts on finalised example data model')
            .get().id.toString()
    }

    @Transactional
    String getFinalisedSimpleDataModelId() {
        DataModel.byLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).get().id.toString()
    }

    Map getValidJsonWithImportedDataType() {
        [
            label          : 'Functional Test DataElement With Imported DataType',
            maxMultiplicity: 1,
            minMultiplicity: 1,
            dataType       : [id: getImportedStringDataTypeId()]
        ]
    }

    void "MI02: Check imported DataElement also imports the DataType"() {
        given:
        loginEditor()

        when: "The save action is executed with valid data"
        POST(getModelImportPath(), [
            importedCatalogueItemDomainType: getImportedCatalogueItemDomainType(),
            importedCatalogueItemId        : getImportedCatalogueItemId()
        ])

        then: "The response is correct"
        verifyResponse CREATED, response
        String id = responseBody().id
        responseBody().catalogueItem.id == getCatalogueItemId()
        responseBody().catalogueItem.domainType == getCatalogueItemDomainType()
        responseBody().importedCatalogueItem.id == getImportedCatalogueItemId()
        responseBody().importedCatalogueItem.domainType == getImportedCatalogueItemDomainType()

        when: 'Getting the model imports list for the DataModel'
        GET(getDataTypeModelImportPath())

        then: 'There is a datatype listed in it'
        responseBody().count == 1
        responseBody().items.first().importedCatalogueItem.id == getImportedStringDataTypeId()
        def dataTypeImportId = responseBody().items.first().id


        when: "List the resources on the DataType endpoint without showing imported resources"
        GET("${getDataTypeResourcePath()}?imported=false")

        then: "The correct resources are listed"
        verifyResponse OK, response
        responseBody().items.every {it.id != getImportedStringDataTypeId()}

        when: "List the resources on the DataType endpoint showing imported resources"
        GET(getDataTypeResourcePath())

        then: "The correct resources are listed"
        verifyResponse OK, response
        responseBody().items.any {it.id == getImportedStringDataTypeId()}

        cleanup:
        DELETE("${getModelImportPath()}/${id}")
        verifyResponse HttpStatus.NO_CONTENT, response

        DELETE("${getDataTypeModelImportPath()}/${dataTypeImportId}")
        verifyResponse HttpStatus.NO_CONTENT, response
    }

    void "MI03: Create DataElement using imported DataType"() {
        given:
        loginEditor()

        when: "Create a new DataElement with the DataType from another model which has not been imported"
        POST(getIndexPath(), getValidJsonWithImportedDataType())

        then: "DataElement is not created"
        verifyResponse HttpStatus.UNPROCESSABLE_ENTITY, response

        when: "The import action for datatype is executed"
        POST(getDataTypeModelImportPath(), [
            importedCatalogueItemDomainType: PrimitiveType.simpleName,
            importedCatalogueItemId        : getImportedStringDataTypeId()
        ])

        then: "The response is correct"
        verifyResponse CREATED, response
        String dataTypeImportId = responseBody().id
        responseBody().catalogueItem.id == getImportingDataModelId()
        responseBody().catalogueItem.domainType == DataModel.simpleName
        responseBody().importedCatalogueItem.id == getImportedStringDataTypeId()
        responseBody().importedCatalogueItem.domainType == PrimitiveType.simpleName

        when: "The ModelImport is requested"
        GET("${getDataTypeModelImportPath()}/${dataTypeImportId}")

        then: "The response is correct"
        verifyResponse OK, response
        responseBody().id == dataTypeImportId
        responseBody().catalogueItem.id == getImportingDataModelId()
        responseBody().catalogueItem.domainType == DataModel.simpleName
        responseBody().importedCatalogueItem.id == getImportedStringDataTypeId()
        responseBody().importedCatalogueItem.domainType == PrimitiveType.simpleName

        when: "List the resources on the DataType endpoint without showing imported resources"
        GET("${getDataTypeResourcePath()}?imported=false")

        then: "The correct resources are listed"
        verifyResponse OK, response
        responseBody().items.every {it.id != getImportedStringDataTypeId()}

        when: "List the resources on the DataType endpoint showing imported resources"
        GET(getDataTypeResourcePath())

        then: "The correct resources are listed"
        verifyResponse OK, response
        responseBody().items.any {it.id == getImportedStringDataTypeId()}

        when: "Create a new DataElement with the imported DataType"
        POST(getIndexPath(), getValidJsonWithImportedDataType())

        then: "DataElement is created correctly"
        verifyResponse CREATED, response
        String newDataElementId = responseBody().id

        when: "Get the DataElement created with the imported DataType"
        GET("${getIndexPath()}/${newDataElementId}")

        then: "The DataElement is shown correctly"
        verifyResponse OK, response
        responseBody().id == newDataElementId

        cleanup:
        DELETE("${getDataTypeModelImportPath()}/${dataTypeImportId}")
        verifyResponse HttpStatus.NO_CONTENT, response
    }
}
