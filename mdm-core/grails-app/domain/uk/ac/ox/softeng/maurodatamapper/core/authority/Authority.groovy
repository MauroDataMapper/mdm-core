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
package uk.ac.ox.softeng.maurodatamapper.core.authority

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

class Authority implements MdmDomain, InformationAware, SecurableResource, EditHistoryAware {

    public static final String DEFAULT_NAME_CONFIG_PROPERTY = 'maurodatamapper.authority.name'
    public static final String DEFAULT_URL_CONFIG_PROPERTY = 'maurodatamapper.authority.url'

    UUID id
    String url
    Boolean readableByEveryone
    Boolean readableByAuthenticatedUsers
    Boolean defaultAuthority

    //    static hasMany = [
    //        models          : Model,
    //        versionedFolders: VersionedFolder
    //    ]

    static constraints = {
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label unique: 'url'
        url blank: false
        defaultAuthority validator: {val, obj ->
            if (val && Authority.countByDefaultAuthorityAndIdNotEqual(true, obj.id)) {
                return ['invalid.authority.default']
            }
        }
    }

    static mapping = {
        defaultAuthority defaultValue: false
    }

    Authority() {
        readableByAuthenticatedUsers = false
        readableByEveryone = false
        defaultAuthority = false
    }

    @Override
    String getDomainType() {
        Authority.simpleName
    }

    @Override
    String getPathPrefix() {
        'auth'
    }

    @Override
    String getPathIdentifier() {
        "${label}@${url}"
    }

    @Override
    String toString() {
        getPathIdentifier()
    }

    @Override
    String getEditLabel() {
        getPathIdentifier()
    }
}
