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
package uk.ac.ox.softeng.maurodatamapper.ui.providers

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.core.support.proxy.ProxyHandler
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType

@CompileStatic
abstract class UIProviderService extends MauroDataMapperService implements Cloneable {

    @Autowired
    MetadataService metadataService

    @Autowired
    SessionFactory sessionFactory

    UIProviderService() {
    }


    @Override
    String getProviderType() {
        'UIProvider'
    }

    @CompileDynamic
    private ParameterizedType getParameterizedTypeSuperClass(def clazz) {
        if (clazz instanceof ParameterizedType) return clazz
        getParameterizedTypeSuperClass(clazz.genericSuperclass)
    }

}