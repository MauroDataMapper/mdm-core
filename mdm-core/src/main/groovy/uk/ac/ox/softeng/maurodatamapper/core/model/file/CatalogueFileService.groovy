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
package uk.ac.ox.softeng.maurodatamapper.core.model.file

import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j

import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.nio.file.Path

@Slf4j
trait CatalogueFileService<T> {

    abstract T createNewFile(String name, byte[] contents, String type, User user)

    def <K extends CatalogueFile> Class<K> getCatalogueFileClass() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().genericInterfaces.find {genericInterface ->
            genericInterface instanceof ParameterizedType &&
            CatalogueFileService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
        }

        (Class<K>) parameterizedType?.actualTypeArguments[0]
    }

    T createNewFile(File file, String type, User user) {
        createNewFile(file.name, file.bytes, type, user)
    }

    T createNewFile(Path filePath, User user) {
        createNewFile(filePath.toFile(), Files.probeContentType(filePath) ?: 'Unknown', user)
    }

    T createNewFileBase(String name, byte[] contents, String type, String userEmail) {
        Class<? extends CatalogueFile> clazz = catalogueFileClass
        CatalogueFile instance = clazz.getDeclaredConstructor().newInstance()
        instance.fileName = name
        instance.fileContents = Utils.copyOf(contents)
        instance.fileType = type ?: 'Unknown'
        instance.fileSize = contents.length
        instance.createdBy = userEmail
        instance as T
    }

}