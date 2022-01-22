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
package uk.ac.ox.softeng.maurodatamapper.hibernate

import uk.ac.ox.softeng.maurodatamapper.path.Path

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor

/**
 * @since 09/09/2021
 */
class PathUserTypeDescriptor extends AbstractTypeDescriptor<Path> {

    public static final PathUserTypeDescriptor INSTANCE = new PathUserTypeDescriptor()

    protected PathUserTypeDescriptor() {
        super(Path)
    }

    @Override
    Path fromString(String string) {
        Path.from(string)
    }

    @Override
    <X> X unwrap(Path value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (Path.isAssignableFrom(type)) {
            return (X) value
        }
        if (String.isAssignableFrom(type)) {
            return (X) toString(value)
        }
        throw unknownUnwrap(type)
    }

    @Override
    <X> Path wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (String.isInstance(value)) {
            return fromString((String) value)
        }
        if (Path.isInstance(value)) {
            return (Path) value
        }
        throw unknownWrap(value.getClass())
    }
}
