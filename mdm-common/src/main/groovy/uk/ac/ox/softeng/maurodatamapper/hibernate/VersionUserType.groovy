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
package uk.ac.ox.softeng.maurodatamapper.hibernate

import uk.ac.ox.softeng.maurodatamapper.version.Version

import org.hibernate.dialect.Dialect
import org.hibernate.type.AbstractSingleColumnStandardBasicType
import org.hibernate.type.DiscriminatorType
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor

/**
 * @since 25/01/2018
 */
class VersionUserType extends AbstractSingleColumnStandardBasicType<Version> implements DiscriminatorType<Version> {

    public static final VersionUserType INSTANCE = new VersionUserType()

    VersionUserType() {
        super(VarcharTypeDescriptor.INSTANCE, VersionUserTypeDescriptor.INSTANCE)
    }

    @Override
    Version stringToObject(String xml) throws Exception {
        fromString(xml)
    }

    @Override
    String objectToSQLString(Version value, Dialect dialect) throws Exception {
        toString(value)
    }

    @Override
    String getName() {
        'version'
    }
}
