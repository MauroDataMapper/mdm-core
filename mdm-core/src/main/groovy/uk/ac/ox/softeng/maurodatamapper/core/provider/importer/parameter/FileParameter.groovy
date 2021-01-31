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
package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter

/**
 * @since 06/03/2018
 */
class FileParameter {

    byte[] fileContents
    String fileName
    String fileType

    FileParameter() {

    }

    FileParameter(String fileName, String fileType, byte[] fileContents) {
        this.fileName = fileName
        this.fileType = fileType
        this.fileContents = fileContents.clone()
    }

    byte[] getFileContents() {
        fileContents.clone()
    }

    void setFileContents(byte[] fileContents) {
        this.fileContents = fileContents.clone()
    }

    InputStream getInputStream() {
        new ByteArrayInputStream(fileContents)
    }
}
