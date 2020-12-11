/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.traits.provider.importer

import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import java.nio.charset.Charset

/**
 * @since 30/11/2020
 */
@Slf4j
trait JsonImportMapping {

    /**
     * Helps with importing json by removing any elements called 'id'
     */
    def slurpAndClean(byte[] content) {
        log.debug('Parsing in file content using JsonSlurper')
        
        //Make an object by slurping json
        def slurped = new JsonSlurper().parseText(new String(content, Charset.defaultCharset()))

        //Serialize the object but without any 'id' elements
        def generator = new JsonGenerator.Options()
            .excludeFieldsByName('id')
            .build()
        
        def serialized = generator.toJson(slurped)

        //Slurp the serialized object. So now we have the same object but without any id elements
        def result = new JsonSlurper().parseText(serialized)

        result
    }
}