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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource
import org.springframework.context.annotation.ComponentScan

@PluginSource
@ComponentScan(basePackages = ['uk.ac.ox.softeng.maurodatamapper'])
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        Utils.outputRuntimeArgs(Application)
        GrailsApp.run(Application, args)
    }
}