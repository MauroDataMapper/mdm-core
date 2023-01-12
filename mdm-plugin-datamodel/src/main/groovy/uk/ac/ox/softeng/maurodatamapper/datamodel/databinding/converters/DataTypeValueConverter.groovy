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
package uk.ac.ox.softeng.maurodatamapper.datamodel.databinding.converters

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ModelDataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.DataBindingSource
import grails.databinding.converters.ValueConverter
import grails.web.databinding.DataBindingUtils
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.BindingResult
import org.springframework.validation.ObjectError

/**
 * @since 16/11/2017
 */
@Slf4j
@CompileStatic
class DataTypeValueConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        value instanceof Map || value instanceof DataBindingSource || (value instanceof Serializable && Utils.toUuid(value))
    }

    @Override
    def convert(Object value) {
        DataType dataType
        if (value instanceof Serializable && Utils.toUuid(value)) {
            dataType = DataType.get(value)
            if (!dataType) throw new ApiBadRequestException('DTB02', 'Provided id cannot be found')
        } else {
            Map map = value as Map
            dataType = DataType.get(map.id as Serializable)
            BindingResult bindingResult = new BeanPropertyBindingResult(dataType, 'DataType')
            if (!dataType) {
                String domainType = map.domainType
                switch (domainType) {
                    case DataType.PRIMITIVE_DOMAIN_TYPE:
                        dataType = new PrimitiveType()
                        break
                    case DataType.ENUMERATION_DOMAIN_TYPE:
                        dataType = new EnumerationType()
                        break
                    case DataType.REFERENCE_DOMAIN_TYPE:
                        dataType = new ReferenceType()
                        break
                    case DataType.MODEL_DATA_DOMAIN_TYPE:
                        dataType = new ModelDataType()
                        break
                    default:
                        String defaultMessage = 'Cannot bind DataType with unknown domainType [{0}]'
                        Object[] arguments = [domainType]
                        String[] codes = getMessageCodes('unknownDomainType', DataType)
                        bindingResult.addError(new ObjectError(bindingResult.getObjectName(), codes, arguments, defaultMessage))
                        throw new ApiInvalidModelException('DTBXX', "Cannot bind DataType with unknown domainType [${domainType}]", bindingResult)
                }

                if (!dataType) {
                    String defaultMessage = 'Cannot bind DataType as id or domainType are not provided'
                    Object[] arguments = []
                    String[] codes = getMessageCodes('noIdOrDomainType', DataType)
                    bindingResult.addError(new ObjectError(bindingResult.getObjectName(), codes, arguments, defaultMessage))
                    throw new ApiInvalidModelException('DTB01', defaultMessage, bindingResult)
                }

                DataBindingUtils.bindObjectToInstance(dataType, value)
            } else if (map.size() > 1) {
                // More properties than id so we perform a full update using binding
                DataBindingUtils.bindObjectToInstance(dataType, value)
            }
        }

        dataType
    }

    @Override
    Class<?> getTargetType() {
        DataType
    }

    protected static String[] getMessageCodes(String messageCode,
                                              Class objectType) {
        String[] codes = [objectType.getName() + '.' + messageCode, messageCode]
        codes
    }
}
