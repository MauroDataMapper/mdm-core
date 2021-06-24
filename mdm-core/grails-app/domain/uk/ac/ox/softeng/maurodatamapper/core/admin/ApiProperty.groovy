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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

import grails.gorm.DetachedCriteria

class ApiProperty implements CreatorAware {

    UUID id
    String key
    String value
    String lastUpdatedBy
    Boolean publiclyVisible
    String category

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        lastUpdatedBy email: true
        publiclyVisible nullable: false
        key blank: false, unique: true, validator: {val ->
            val.matches(/[a-z._]+/) ?: ['invalid.api.property.format']
        }
        value blank: false, validator: {val, obj ->
            if (obj.key == ApiPropertyEnum.SITE_URL.key) {
                try {
                    val.toURL()
                    true
                } catch (MalformedURLException ignored) {
                    ['default.invalid.url.message']
                }
            }
        }
        category nullable: true, blank: false
    }

    static mapping = {
        value type: 'text'
    }

    ApiProperty() {
        publiclyVisible = false
    }

    @Override
    String getDomainType() {
        ApiProperty.simpleName
    }

    @Override
    String getPathPrefix() {
        'api'
    }

    def beforeValidate() {
        if (!lastUpdatedBy) lastUpdatedBy = createdBy
    }

    def beforeInsert() {
        if (!lastUpdatedBy) lastUpdatedBy = createdBy
    }

    static DetachedCriteria<ApiProperty> by() {
        new DetachedCriteria<ApiProperty>(ApiProperty)
    }

    static DetachedCriteria<ApiProperty> byPubliclyVisible() {
        by().eq('publiclyVisible', true)
    }

    static String extractDefaultCategoryFromKey(String key) {
        key.split(/\./)[0].capitalize()
    }
}
