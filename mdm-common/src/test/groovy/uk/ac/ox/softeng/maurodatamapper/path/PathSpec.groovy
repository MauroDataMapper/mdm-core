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
package uk.ac.ox.softeng.maurodatamapper.path

import spock.lang.Specification

/**
 * @since 20/07/2021
 */
class PathSpec extends Specification {

    void 'test single path creation'() {
        when:
        Path path = Path.from('dm:test')

        then:
        path.first().prefix == 'dm'
        path.first().identifier == 'test'

        when:
        path = Path.from('dm:test$main')

        then:
        path.first().prefix == 'dm'
        path.first().identifier == 'test'
        path.first().modelIdentifier == 'main'

        when:
        path = Path.from('dm:test@description')

        then:
        path.first().prefix == 'dm'
        path.first().identifier == 'test'
        !path.first().modelIdentifier
        path.first().attribute == 'description'

        when:
        path = Path.from('dm:test$main@description')

        then:
        path.first().prefix == 'dm'
        path.first().identifier == 'test'
        path.first().modelIdentifier == 'main'
        path.first().attribute == 'description'
    }

    void 'test depth path creation'() {
        when:
        Path path = Path.from('dm:test$main|dc:test2|de:test3')

        then:
        path.size() == 3
        path[0].prefix == 'dm'
        path[0].identifier == 'test'
        path[1].prefix == 'dc'
        path[1].identifier == 'test2'
        path[2].prefix == 'de'
        path[2].identifier == 'test3'

        and:
        path.matches(Path.from('dm:test$main|dc:test2|de:test3'))
        path.isAbsoluteTo(Path.from('dm:test$main'))
        !path.isAbsoluteTo(Path.from('dm:test$test'))

        when:
        path = Path.from('dm:test$main|dc:test2|de:test3@description')

        then:
        path.size() == 3
        path[0].prefix == 'dm'
        path[0].identifier == 'test'
        path[0].modelIdentifier == 'main'
        path[1].prefix == 'dc'
        path[1].identifier == 'test2'
        path[2].prefix == 'de'
        path[2].identifier == 'test3'
        path[2].attribute == 'description'

        and:
        path.matches(Path.from('dm:test$main|dc:test2|de:test3'))

        when:
        path = Path.from('dm:test|dc:test2|de:test3')

        then:
        path.size() == 3
        path[0].prefix == 'dm'
        path[0].identifier == 'test'
        path[1].prefix == 'dc'
        path[1].identifier == 'test2'
        path[2].prefix == 'de'
        path[2].identifier == 'test3'

        and:
        path.matches(Path.from('dm:test$main|dc:test2|de:test3'))
        path.isAbsoluteTo(Path.from('dm:test'))

    }

    void 'child path testing'() {
        given:
        Path path = Path.from('dm:test$main|dc:test2|de:test3')

        when:
        Path childPath = path.childPath

        then:
        childPath.size() == 2
        childPath.first().prefix == 'dc'
        childPath.first().identifier == 'test2'
    }
}
