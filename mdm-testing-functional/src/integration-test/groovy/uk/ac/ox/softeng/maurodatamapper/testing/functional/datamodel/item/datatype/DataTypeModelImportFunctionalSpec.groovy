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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.ModelImportFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Ignore

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * <pre>
 * Controller: dataType
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes        | Action: save
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes        | Action: index
 *  |  DELETE  | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: delete
 *  |  PUT     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: update
 *  |  GET     | /api/dataModels/${dataModelId}/dataTypes/${id}  | Action: show
 *
 *  |  POST    | /api/dataModels/${dataModelId}/dataTypes/${otherModelId}/${dataTypeId}  | Action: copyDataType
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeController
 */
@Integration
@Slf4j
@Ignore('No longer relevant')
class DataTypeModelImportFunctionalSpec extends ModelImportFunctionalSpec {

    @Override
    String getIndexPath() {
        "dataModels/${getImportingDataModelId()}/dataTypes"
    }

    @Override
    String getModelImportPath() {
        "dataModels/${getImportingDataModelId()}/modelImports"
    }

    @Transactional
    @Override
    String getImportedCatalogueItemId() {
        DataType.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id,
                                       'gnirts on finalised example data model').get().id.toString()
    }

    @Override
    String getImportedCatalogueItemDomainType() {
        PrimitiveType.simpleName
    }

    @Override
    String getCatalogueItemId() {
        getImportingDataModelId()
    }

    @Override
    String getCatalogueItemDomainType() {
        DataModel.simpleName
    }

    @Transactional
    String getImportingDataModelId() {
        DataModel.findByLabel(BootstrapModels.IMPORTING_DATAMODEL_NAME_1).id.toString()
    }

    @Transactional
    String getImportedDataClassId() {
        DataClass.byDataModelIdAndLabel(DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id, BootstrapModels.FIRST_CLASS_LABEL_ON_FINALISED_EXAMPLE_DATAMODEL).get().id.toString()
    }   

    @Transactional
    String getImportedDataModelId() {
        DataModel.findByLabel(BootstrapModels.FINALISED_EXAMPLE_DATAMODEL_NAME).id.toString()
    }

    void "MI02: Test creating a Reference DataType using an imported DataClass"() {
        given:
        loginEditor()

        when: "The save action is executed using valid data with the imported DataClass"
        POST(getIndexPath(), [
            domainType    : 'ReferenceType',
            label         : 'Functional Reference Type on Imported DataClass',
            referenceClass: getImportedDataClassId()
        ])

        then: "The response is unprocessable as the DC is in the wrong datamodel"
        verifyResponse UNPROCESSABLE_ENTITY, response

        when: "A DataClass is imported into the DataModel"
        POST(getModelImportPath(), [
            importedCatalogueItemDomainType: DataClass.simpleName,
            importedCatalogueItemId        : getImportedDataClassId()
        ])

        then: "The response is correct"
        verifyResponse CREATED, response
        String modelImportId = responseBody().id

        when: "The save action is executed using valid data with the imported DataClass"
        POST(getIndexPath(), [
            domainType    : ReferenceType.simpleName,
            label         : 'Functional Reference Type on Imported DataClass',
            referenceClass: getImportedDataClassId()
        ])

        then: "The response is correct"
        verifyResponse CREATED, response
        String id = responseBody().id
        assert responseBody().domainType == ReferenceType.simpleName
        assert responseBody().label == 'Functional Reference Type on Imported DataClass'
        assert responseBody().referenceClass.id == getImportedDataClassId()

        when:
        GET("${getIndexPath()}/${id}")

        then:
        verifyResponse OK, response
        responseBody().id == id
        responseBody().referenceClass.id == getImportedDataClassId()

        when: "List the DataTypes on the importing DataModel"
        GET(getIndexPath())

        then: "The ReferenceType is included in the list as the imported primitive type"
        verifyResponse OK, response
        responseBody().items.any {it.id == id}

        cleanup:
        //Delete the Reference Type
        DELETE("${getIndexPath()}/${id}", MAP_ARG)
        verifyResponse NO_CONTENT, response

        //Delete the ModelImport
        DELETE("${getModelImportPath()}/${modelImportId}", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
    }    

}
