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
package uk.ac.ox.softeng.maurodatamapper.datamodel.provider


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.rest.transport.DefaultDataType
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperProvider

/**
 * @since 18/04/2018
 */
trait DefaultDataTypeProvider implements MauroDataMapperProvider {

    abstract List<DefaultDataType> getDefaultListOfDataTypes()

    abstract String getDisplayName()

    DataModel addDefaultListOfDataTypesToDataModel(DataModel dataModel) {
        defaultListOfDataTypes.each {
            DataType dataType
            switch (it.domainType) {
                case PrimitiveType.simpleName:
                    dataType = new PrimitiveType(units: it.units)
                    break
                case EnumerationType.simpleName:
                    dataType = new EnumerationType(enumerationValues: it.enumerationValues)
                    break
            }
            dataType.createdBy = dataModel.createdBy
            dataType.label = it.label
            dataType.description = it.description
            dataModel.addToDataTypes(dataType)
        }
        dataModel
    }

    @Override
    String getName() {
        getClass().simpleName
    }
}