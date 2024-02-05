/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.provider.plugin

import grails.plugins.GrailsPlugin

/**
 * @since 17/10/2019
 */
class GrailsPluginMauroDataMapperPlugin extends AbstractMauroDataMapperPlugin {

    GrailsPlugin plugin

    @Override
    Closure doWithSpring() {
        null
    }

    @Override
    String getName() {
        "grails.${plugin.name}"
    }

    @Override
    String getVersion() {
        plugin.version
    }

    @Override
    int getOrder() {
        LOWEST_PRECEDENCE - 100
    }

    boolean isMdmModule() {
        false
    }
}