/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.version


import spock.lang.Specification

/**
 * @since 25/01/2017
 */
class VersionSpec extends Specification {

    void 'conversion of version string'() {
        when:
        Version version = Version.from('3.1.2')

        then:
        version.major == 3
        version.minor == 1
        version.patch == 2
        version.snapshot == false

        when:
        version = Version.from('3.1.0')

        then:
        version.major == 3
        version.minor == 1
        version.patch == 0
        version.snapshot == false
    }

    void 'conversion of simple version string'() {
        when:
        Version version = Version.from('3.1')

        then:
        version.major == 3
        version.minor == 1
        version.patch == 0
        version.snapshot == false

        when:
        version = Version.from('3.0')

        then:
        version.major == 3
        version.minor == 0
        version.patch == 0
        version.snapshot == false

        when:
        version = Version.from('0.1')

        then:
        version.major == 0
        version.minor == 1
        version.patch == 0
        version.snapshot == false
    }

    void 'conversion of major only version string'() {
        when:
        Version version = Version.from('3')

        then:
        version.major == 3
        version.minor == 0
        version.patch == 0
        version.snapshot == false

        when:
        version = Version.from('2')

        then:
        version.major == 2
        version.minor == 0
        version.patch == 0
        version.snapshot == false

        when:
        version = Version.from('1')

        then:
        version.major == 1
        version.minor == 0
        version.patch == 0
        version.snapshot == false
    }

    void 'conversion of snapshot version string'(){
        when:
        Version version = Version.from('SNAPSHOT')

        then:
        version.major == 0
        version.minor == 0
        version.patch == 0
        version.snapshot == true

        and:
        version.toString() == 'SNAPSHOT'

        when:
        version = Version.from('1.0.0-SNAPSHOT')

        then:
        version.major == 1
        version.minor == 0
        version.patch == 0
        version.snapshot == true

        and:
        version.toString() == '1.0.0-SNAPSHOT'

        when:
        version = Version.from('1.2.3-SNAPSHOT')

        then:
        version.major == 1
        version.minor == 2
        version.patch == 3
        version.snapshot == true

        and:
        version.toString() == '1.2.3-SNAPSHOT'
    }

    void 'comparison of versions'() {

        when:
        Version v1 = Version.from('3.1.2')
        Version v2 = Version.from('4.0.0')

        then:
        v2 > v1

        when:
        v1 = Version.from('3.1.2')
        v2 = Version.from('3.1.2')

        then:
        v2 == v1

        when:
        v1 = Version.from('3.1.2')
        v2 = Version.from('3.2.0')

        then:
        v2 > v1

        when:
        v1 = Version.from('3.1.2')
        v2 = Version.from('3.1.3')

        then:
        v2 > v1

        when:
        v1 = Version.from('3.1.3')
        v2 = Version.from('3.1.2')

        then:
        v2 < v1

        when:
        v1 = Version.from('3.1.2')
        v2 = Version.from('3.1.3')

        then:
        v2 >= v1

        when:
        v1 = Version.from('3.0.0')
        v2 = Version.from('3.0.0')

        then:
        v2 == v1
    }

    void 'comparison of simple versions'() {
        when:
        Version v1 = Version.from('0.1')
        Version v2 = Version.from('0.2')

        then:
        v2 > v1

        when:
        v1 = Version.from('0.1')
        v2 = Version.from('0.1')

        then:
        v2 == v1

        when:
        v1 = Version.from('0.1')
        v2 = Version.from('1.0')

        then:
        v2 > v1

        when:
        v1 = Version.from('0.1')
        v2 = Version.from('0.1.1')

        then:
        v2 > v1

        when:
        v1 = Version.from('1.0')
        v2 = Version.from('0.2')

        then:
        v2 < v1

        when:
        v1 = Version.from('0.1')
        v2 = Version.from('0.2')

        then:
        v2 >= v1

        when:
        v1 = Version.from('3.0')
        v2 = Version.from('3.0.0')

        then:
        v2 == v1
    }

    void 'comparison of major only versions'() {
        when:
        Version v1 = Version.from('1')
        Version v2 = Version.from('2')

        then:
        v2 > v1

        when:
        v1 = Version.from('1')
        v2 = Version.from('1')

        then:
        v2 == v1

        when:
        v1 = Version.from('1')
        v2 = Version.from('1.0.1')

        then:
        v2 > v1

        when:
        v1 = Version.from('1')
        v2 = Version.from('0.2')

        then:
        v2 < v1

        when:
        v1 = Version.from('0.1')
        v2 = Version.from('2')

        then:
        v2 >= v1

        when:
        v1 = Version.from('3')
        v2 = Version.from('3.0.0')

        then:
        v2 == v1
    }

    void 'test next major version'() {
        when:
        Version v = Version.from('1')
        Version nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('2.0.0')

        when:
        v = Version.from('0.0.1')
        nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('1.0.0')

        when:
        v = Version.from('0.2.0')
        nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('1.0.0')

        when:
        v = Version.from('1.0.1')
        nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('2.0.0')

        when:
        v = Version.from('1.1.0')
        nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('2.0.0')

        when:
        v = Version.from('1.2.1')
        nm = Version.nextMajorVersion(v)

        then:
        nm == Version.from('2.0.0')
    }

    void 'test next minor version'() {
        when:
        Version v = Version.from('1')
        Version nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('1.1.0')

        when:
        v = Version.from('0.0.1')
        nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('0.1.0')

        when:
        v = Version.from('0.2.0')
        nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('0.3.0')

        when:
        v = Version.from('1.0.1')
        nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('1.1.0')

        when:
        v = Version.from('1.1.0')
        nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('1.2.0')

        when:
        v = Version.from('1.2.1')
        nm = Version.nextMinorVersion(v)

        then:
        nm == Version.from('1.3.0')
    }

    void 'test next patch version'() {
        when:
        Version v = Version.from('1')
        Version nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('1.0.1')

        when:
        v = Version.from('0.0.1')
        nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('0.0.2')

        when:
        v = Version.from('0.2.0')
        nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('0.2.1')

        when:
        v = Version.from('1.0.1')
        nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('1.0.2')

        when:
        v = Version.from('1.1.0')
        nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('1.1.1')

        when:
        v = Version.from('1.2.1')
        nm = Version.nextPatchVersion(v)

        then:
        nm == Version.from('1.2.2')
    }
}
