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
package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperProvider
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.GrailsPluginMauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.JavaModule
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.MauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.MdmGrailsPluginMauroDataMapperPlugin

import grails.plugins.GrailsPluginManager
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 17/08/2017
 */
class MauroDataMapperProviderService {

    @Autowired(required = false)
    Set<MauroDataMapperPlugin> moduleBeans

    GrailsPluginManager pluginManager

    MauroDataMapperPlugin findModule(String name, String version) {
        modulesList.find {it.name.equalsIgnoreCase(name) && it.version.equalsIgnoreCase(version)}
    }

    Set<GrailsPluginMauroDataMapperPlugin> getAllGrailsPluginModules() {
        pluginManager.allPlugins.collect {
            it.name.startsWith('mdm') ? new MdmGrailsPluginMauroDataMapperPlugin(plugin: it) : new GrailsPluginMauroDataMapperPlugin(plugin: it)
        }
    }

    Set<GrailsPluginMauroDataMapperPlugin> getGrailsPluginModules() {
        allGrailsPluginModules.findAll {!it.isMdmModule()}
    }

    Set<GrailsPluginMauroDataMapperPlugin> getMdmGrailsPluginModules() {
        allGrailsPluginModules.findAll {it.isMdmModule()}
    }

    Set<JavaModule> getJavaModules() {
        ModuleLayer.boot().modules().collect {new JavaModule(module: it)}
    }

    Set<AbstractMauroDataMapperPlugin> getOtherModules() {
        getServices(MauroDataMapperPlugin,
                    moduleBeans) as Set<AbstractMauroDataMapperPlugin>
    }

    List<MauroDataMapperPlugin> getModulesList() {
        (otherModules + allGrailsPluginModules + javaModules).sort() as List<MauroDataMapperPlugin>
    }

    static <T extends MauroDataMapperProvider, P extends T> Set<T> getServices(Class<P> providerType, Set<T> beans = []) {
        beans ? beans + ServiceLoader.load(providerType).collect {it} as HashSet : ServiceLoader.load(providerType).collect {it} as HashSet

    }
}
