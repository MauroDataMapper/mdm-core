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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType

@CompileStatic
abstract class ProfileProviderService<P extends Profile, D extends CatalogueItem> extends MauroDataMapperService {

    @Autowired
    MetadataService metadataService

    abstract void storeProfileInEntity(D entity, P profile, String userEmailAddress)

    abstract P createProfileFromEntity(D entity)

    abstract String getMetadataNamespace()

    boolean isJsonProfileService() { return true }

    @Override
    String getProviderType() {
        'ProfileProvider'
    }

    @Override
    Set<String> getKnownMetadataKeys() {
        getNewProfile().knownFields
    }

    @Override
    Boolean allowsExtraMetadataKeys() {
        false
    }

    P getNewProfile() {
        getProfileClass().getDeclaredConstructor().newInstance()
    }

    P getAllProfileFieldValues(Map params, List<P> profiles) {
        Set<String> filters = []
        P returnProfile = getNewProfile()

        if (!params['filter']) {
            filters = returnProfile.getKnownFields()
        } else if (params['filter'] instanceof String) {
            filters = [params['filter'].toString()] as Set
        } else {
            filters.addAll((List<String>) params['filter'])
        }

        profiles.each { profile ->

            returnProfile.getKnownFields().each { profileFieldName ->
                if (profileFieldName in filters && profile[profileFieldName]) {

                    Set returnValue = returnProfile[profileFieldName] as Set

                    if (!returnValue) returnValue = new HashSet()

                    returnValue.add(profile[profileFieldName])

                    returnProfile[profileFieldName] = returnValue
                }
            }
        }
        returnProfile
    }

    Class<P> getProfileClass() {
        ParameterizedType parameterizedType = getParameterizedTypeSuperClass(this.getClass())
        Class<P> resourceClass = (Class<P>) parameterizedType?.actualTypeArguments[0]
        if (!Profile.isAssignableFrom(resourceClass)) {
            throw new IllegalStateException("Service Class ${resourceClass.simpleName} does not extend Profile")
        }
        resourceClass
    }

    void storeProfileInEntity(D entity, P profile, User user) {
        storeProfileInEntity(entity, profile, user.emailAddress)
    }

    @CompileDynamic
    private ParameterizedType getParameterizedTypeSuperClass(def clazz) {
        if (clazz instanceof ParameterizedType) return clazz
        getParameterizedTypeSuperClass(clazz.genericSuperclass)
    }

    List<String> profileApplicableForDomains() {
        return []
    }

}