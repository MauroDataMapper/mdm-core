/*
 * Copyright 2020-2023 University of Oxford and NHS England
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

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor

/**
 * @since 25/01/2018
 */
class VersionUserTypeDescriptor extends AbstractTypeDescriptor<Version> {

    public static final VersionUserTypeDescriptor INSTANCE = new VersionUserTypeDescriptor()

    VersionUserTypeDescriptor() {
        super(Version)
    }

    @Override
    String toString(Version value) {
        value.toString()
    }

    @Override
    Version fromString(String string) {
        Version.from(string)
    }

    @Override
    <X> X unwrap(Version value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (Version.isAssignableFrom(type)) {
            return (X) value
        }
        if (String.isAssignableFrom(type)) {
            return (X) toString(value)
        }
        throw unknownUnwrap(type)
    }

    @Override
    <X> Version wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (String.isInstance(value)) {
            return fromString((String) value)
        }
        if (Version.isInstance(value)) {
            return (Version) value
        }
        throw unknownWrap(value.getClass())
    }
}
