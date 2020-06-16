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
package uk.ac.ox.softeng.maurodatamapper.datamodel.databinding

import uk.ac.ox.softeng.maurodatamapper.datamodel.databinding.converters.DataTypeValueConverter
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType

import grails.databinding.BindingHelper
import grails.databinding.DataBindingSource
import groovy.transform.CompileStatic

/**
 * @since 16/11/2017
 */
@CompileStatic
class DataTypeBindingHelper implements BindingHelper<DataType> {

    private static final DataTypeValueConverter converter = new DataTypeValueConverter()

    @Override
    DataType getPropertyValue(Object obj, String propertyName, DataBindingSource source) {
        if (converter.canConvert(source.getPropertyValue(propertyName))) {
            return converter.convert(source.getPropertyValue(propertyName)) as DataType
        }
        source.getPropertyValue(propertyName) as DataType
    }
}
