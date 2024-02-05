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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ConfigurableProfileFieldTypes
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.BootStrap.BootStrapUser

@Slf4j
class BootStrap {

    ApiPropertyService apiPropertyService
    MessageSource messageSource

    def init = { servletContext ->

        try {
            loadAndSetConfigurableFieldType(ConfigurableProfileFieldTypes.DATE_FORMAT_KEY,
                                            ConfigurableProfileFieldTypes.DATE_FORMAT_DEFAULT,
                                            ConfigurableProfileFieldTypes.instance.dateFormats)
            loadAndSetConfigurableFieldType(ConfigurableProfileFieldTypes.DATETIME_FORMAT_KEY,
                                            ConfigurableProfileFieldTypes.DATETIME_FORMAT_DEFAULT,
                                            ConfigurableProfileFieldTypes.instance.dateTimeFormats)
            loadAndSetConfigurableFieldType(ConfigurableProfileFieldTypes.TIME_FORMAT_KEY,
                                            ConfigurableProfileFieldTypes.TIME_FORMAT_DEFAULT,
                                            ConfigurableProfileFieldTypes.instance.timeFormats)
        } catch (Exception ignored) {
            log.warn('Couldn\'t load configurable profile field types from ApiProperties, will use defaults')
        }
    }
    def destroy = {
    }

    void loadAndSetConfigurableFieldType(String key, String[] defaultValues, String[] target) {
        ApiProperty.withNewTransaction {
            ApiProperty apiProperty = apiPropertyService.findByKey(key)
            if (!apiProperty) {
                apiProperty = new ApiProperty(key: key,
                                              value: defaultValues.join(','),
                                              publiclyVisible: false,
                                              category: 'profile',
                                              lastUpdatedBy: BootStrapUser.instance.emailAddress,
                                              createdBy: BootStrapUser.instance.emailAddress)
                GormUtils.checkAndSave(messageSource, apiProperty)
            }
            target = apiProperty.value.split(',')
        }
    }
}
