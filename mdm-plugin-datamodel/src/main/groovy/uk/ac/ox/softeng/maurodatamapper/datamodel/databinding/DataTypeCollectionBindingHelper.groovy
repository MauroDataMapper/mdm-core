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
package uk.ac.ox.softeng.maurodatamapper.datamodel.databinding

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType

import grails.databinding.BindingHelper
import grails.databinding.DataBindingSource
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/**
 * @since 16/11/2017
 */
@CompileStatic
class DataTypeCollectionBindingHelper implements BindingHelper<Set<DataType>> {

    @Override
    Set<DataType> getPropertyValue(Object obj, String propertyName, DataBindingSource source) {
        if (source.getPropertyValue(propertyName) instanceof Collection) {
            if (obj instanceof DataModel) {
                Collection sourceToBind = source.getPropertyValue(propertyName) as Collection
                DataModel dataModel = obj as DataModel
                return getPropertyValue(dataModel, sourceToBind)
            }
            throw new GroovyCastException(obj, DataModel)
        }
        throw new GroovyCastException(source.getPropertyValue(propertyName), Collection)
    }

    Set<DataType> getPropertyValue(DataModel dataModel, Collection sourceToBind) {
        Set<DataType> dataTypes = new HashSet<>(sourceToBind.size())
        sourceToBind.each { stb ->
            DataType dt = new DataTypeBindingHelper().getPropertyValue(stb)
            dataModel.addToDataTypes(dt)
            dataTypes << dt
        }
        dataTypes
    }

}
