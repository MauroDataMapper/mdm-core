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
package uk.ac.ox.softeng.maurodatamapper.referencedata.databinding.converters

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
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
class ReferenceModelDataTypeValueConverter implements ValueConverter {
    @Override
    boolean canConvert(Object value) {
        value instanceof Map || value instanceof DataBindingSource || (value instanceof Serializable && Utils.toUuid(value))
    }

    @Override
    def convert(Object value) {
        ReferenceDataType dataType
        if (value instanceof Serializable && Utils.toUuid(value)) {
            dataType = ReferenceDataType.get(value)
            if (!dataType) throw new ApiBadRequestException('DTB02', 'Provided id cannot be found')
        } else {
            Map map = value as Map
            dataType = ReferenceDataType.get(map.id as Serializable)
            BindingResult bindingResult = new BeanPropertyBindingResult(dataType, 'DataType')
            if (!dataType) {
                String domainType = map.domainType
                switch (domainType) {
                    case ReferenceDataType.PRIMITIVE_DOMAIN_TYPE:
                        dataType = new ReferencePrimitiveType()
                        break
                    case ReferenceDataType.ENUMERATION_DOMAIN_TYPE:
                        dataType = new ReferenceEnumerationType()
                        break
                    default:
                        String defaultMessage = 'Cannot bind DataType with unknown domainType [{}]'
                        Object[] arguments = [domainType]
                        String[] codes = getMessageCodes('unknownDomainType', ReferenceDataType)
                        bindingResult.addError(new ObjectError(bindingResult.getObjectName(), codes, arguments, defaultMessage))
                        throw new ApiInvalidModelException('DTBXX', "Cannot bind DataType with unknown domainType [${domainType}]", bindingResult)
                }

                if (!dataType) {
                    String defaultMessage = 'Cannot bind DataType as id or domainType are not provided'
                    Object[] arguments = []
                    String[] codes = getMessageCodes('noIdOrDomainType', ReferenceDataType)
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
        ReferenceDataType
    }

    protected static String[] getMessageCodes(String messageCode,
                                              Class objectType) {
        String[] codes = [objectType.getName() + '.' + messageCode, messageCode]
        codes
    }
}
