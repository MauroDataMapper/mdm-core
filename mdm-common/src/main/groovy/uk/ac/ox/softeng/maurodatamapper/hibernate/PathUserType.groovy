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

import uk.ac.ox.softeng.maurodatamapper.path.Path

import org.hibernate.dialect.Dialect
import org.hibernate.type.AbstractSingleColumnStandardBasicType
import org.hibernate.type.DiscriminatorType
import org.hibernate.type.descriptor.sql.LongVarcharTypeDescriptor

/**
 * @since 09/09/2021
 */
class PathUserType extends AbstractSingleColumnStandardBasicType<Path> implements DiscriminatorType<Path> {

    public static final PathUserType INSTANCE = new PathUserType()

    PathUserType() {
        super(LongVarcharTypeDescriptor.INSTANCE, PathUserTypeDescriptor.INSTANCE)
    }

    @Override
    Path stringToObject(String xml) throws Exception {
        try {
            Path.from(xml)
        } catch (Exception ignored) {
            null
        }
    }

    @Override
    String objectToSQLString(Path value, Dialect dialect) throws Exception {
        value.toString()
    }

    @Override
    String getName() {
        'path'
    }
}
