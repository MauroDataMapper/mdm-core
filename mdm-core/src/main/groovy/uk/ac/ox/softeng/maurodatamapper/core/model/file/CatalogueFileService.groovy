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
package uk.ac.ox.softeng.maurodatamapper.core.model.file

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.util.logging.Slf4j

import java.awt.image.BufferedImage
import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

import static java.awt.RenderingHints.KEY_INTERPOLATION
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC

@Slf4j
trait CatalogueFileService<T> {

    abstract T createNewFile(String name, byte[] contents, String type, User user)

    abstract T resizeImage(T catalogueFile, int size)

    def <K extends CatalogueFile> Class<K> getCatalogueFileClass() {
        ParameterizedType parameterizedType = (ParameterizedType) getClass().genericInterfaces.find {genericInterface ->
            genericInterface instanceof ParameterizedType &&
            CatalogueFileService.isAssignableFrom((Class) ((ParameterizedType) genericInterface).rawType)
        }

        (Class<K>) parameterizedType?.actualTypeArguments[0]
    }

    void writeToFile(CatalogueFile file, String filePath) throws ApiException {
        Path path = Paths.get(filePath, file.fileName)
        try {
            Files.write(path, file.fileContents)
        } catch (IOException e) {
            throw new ApiInternalException('CFS01', "Cannot output file to ${path}", e);
        }
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
        instance.fileContents = contents.clone()
        instance.fileType = type ?: 'Unknown'
        instance.fileSize = contents.length
        instance.createdBy = userEmail
        instance as T
    }

    def <K extends CatalogueFile> T resizeImageBase(K catalogueFile, int size) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(catalogueFile.fileContents))
        if (!image) return catalogueFile as T
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        def resize = new BufferedImage(size, size, image.type)
        resize.createGraphics().with {
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            drawImage(image, 0, 0, size, size, null)
            dispose()
        }
        ImageIO.write(resize, 'png', outputStream)
        createNewFileBase(catalogueFile.fileName, outputStream.toByteArray(), 'image/png', catalogueFile.createdBy)
    }
}