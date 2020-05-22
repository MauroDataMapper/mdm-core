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
package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import io.micronaut.core.type.Argument

import static io.micronaut.http.HttpStatus.OK

/**
 * @see MauroDataMapperProviderController* |   GET   | /api/admin/modules              | Action: modules              |
 * @since 28/10/2019
 */
@Integration
class MauroDataMapperProviderFunctionalSpec extends BaseFunctionalSpec {

    @Override
    String getResourcePath() {
        'admin'
    }

    void 'test get modules'() {
        when:
        GET('modules', Argument.of(String))

        then:
        verifyJsonResponse(OK, '''[
  {
    "name": "Common",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "Core",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "assetPipeline",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "cache",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "codecs",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "controllers",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "controllersAsync",
    "version": "SNAPSHOT"
  },
  {
    "name": "converters",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "core",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "dataSource",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "domainClass",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "eventBus",
    "version": "SNAPSHOT"
  },
  {
    "name": "groovyPages",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "hibernate",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "hibernateSearch",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "i18n",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "interceptors",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jsonView",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "markupView",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "restResponder",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "services",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "urlMappings",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.base",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.compiler",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.datatransfer",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.desktop",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.instrument",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.logging",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.management",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.management.rmi",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.naming",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.net.http",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.prefs",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.rmi",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.scripting",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.security.jgss",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.security.sasl",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.smartcardio",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.sql",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.sql.rowset",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.transaction.xa",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.xml",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "java.xml.crypto",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.accessibility",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.attach",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.charsets",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.compiler",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.crypto.cryptoki",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.crypto.ec",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.dynalink",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.editpad",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.httpserver",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.internal.ed",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.internal.jvmstat",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.internal.le",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.internal.opt",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jartool",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.javadoc",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jconsole",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jdeps",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jdi",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jdwp.agent",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jfr",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jlink",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jshell",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jsobject",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.jstatd",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.localedata",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.management",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.management.agent",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.management.jfr",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.naming.dns",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.naming.rmi",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.net",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.scripting.nashorn",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.sctp",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.security.auth",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.security.jgss",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.unsupported",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.unsupported.desktop",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.xml.dom",
    "version": "${json-unit.matches:version}"
  },
  {
    "name": "jdk.zipfs",
    "version": "${json-unit.matches:version}"
  }
]''')
    }

}
