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
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.Profile
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import grails.core.support.proxy.ProxyHandler
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType

@CompileStatic
abstract class ProfileProviderService<P extends Profile, D extends MultiFacetAware> extends MauroDataMapperService implements Cloneable {

    @Autowired
    ProxyHandler proxyHandler

    @Autowired
    MetadataService metadataService

    @Autowired
    SessionFactory sessionFactory

    ProfileProviderService() {
    }

    ProfileProviderService(ProxyHandler proxyHandler, MetadataService metadataService, SessionFactory sessionFactory) {
        this.proxyHandler = proxyHandler
        this.metadataService = metadataService
        this.sessionFactory = sessionFactory
    }

    abstract P storeProfileInEntity(D entity, P profile, String userEmailAddress, boolean isEntityFinalised)

    abstract P createProfileFromEntity(D entity)

    abstract String getMetadataNamespace()

    boolean disabled = false

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

    Boolean canBeEditedAfterFinalisation() {
        false
    }

    String getUniqueIdentifierKey() {
        "${getMetadataNamespace()}.${getName()}.${getVersion()}"
    }

    P getNewProfile() {
        getProfileClass().getDeclaredConstructor().newInstance()
    }

    Class<P> getProfileClass() {
        ParameterizedType parameterizedType = getParameterizedTypeSuperClass(this.getClass())
        Class<P> resourceClass = (Class<P>) parameterizedType?.actualTypeArguments[0]
        if (!Profile.isAssignableFrom(resourceClass)) {
            throw new IllegalStateException("Service Class ${resourceClass.simpleName} does not extend Profile")
        }
        resourceClass
    }

    void storeProfileInEntity(D entity, P profile, String userEmailAddress) {
        storeProfileInEntity(entity, profile, userEmailAddress, false)
    }

    @CompileDynamic
    private ParameterizedType getParameterizedTypeSuperClass(def clazz) {
        if (clazz instanceof ParameterizedType) return clazz
        getParameterizedTypeSuperClass(clazz.genericSuperclass)
    }

    List<String> profileApplicableForDomains() {
        return []
    }

    List<MetadataAware> findAllProfiledItems(String domainType = null) {
        metadataService.findAllMultiFacetAwareItemsByNamespace(metadataNamespace, domainType)
    }

    List<Metadata> getAllProfileMetadataByMultiFacetAwareItemId(UUID multiFacetAwareItemId) {
        metadataService.findAllByMultiFacetAwareItemIdAndNamespace(multiFacetAwareItemId, this.getMetadataNamespace())
    }

    P createCleanProfileFromProfile(P submittedProfile) {
        P cleanProfile = getNewProfile()
        cleanProfile.each {section ->
            ProfileSection submittedSection = submittedProfile.find {it.name == section.name}
            if (submittedSection) {
                section.each {field ->
                    ProfileField submittedField = submittedSection.find {it.getUniqueKey(section.name) == field.getUniqueKey(section.name)}
                    if (submittedField) {
                        field.currentValue = submittedField.currentValue ?: ''
                    }
                }
            }
        }
        cleanProfile
    }

    P createCleanProfileFromFlatMap(Map<String, String> profileData) {
        P cleanProfile = getNewProfile()
        cleanProfile.each {section ->
            section.each {field ->
                Map.Entry<String, String> data = profileData.find {k, v -> k == field.getUniqueKey(section.name)}
                if (data) {
                    field.currentValue = data.value ?: ''
                }
            }
        }
        cleanProfile
    }

    void storeFlatMapProfileInEntity(D entity, String userEmailAddress, Map<String, String> profileData) {
        P profile = createCleanProfileFromFlatMap(profileData)
        storeProfileInEntity(entity, profile, userEmailAddress)
    }

    Map<String, Collection<String>> listAllValuesInProfile(String domainType, List<String> filter, UserSecurityPolicyManager userSecurityPolicyManager) {
        List<MetadataAware> profiledItems = findAllProfiledItems(domainType)
        List<MetadataAware> filteredProfiledItems = []
        profiledItems.each {profiledItem ->
            if (profiledItem instanceof Model
                && userSecurityPolicyManager.userCanReadSecuredResourceId(profiledItem.getClass(), profiledItem.id)) {
                filteredProfiledItems.add(profiledItem)
            } else if (profiledItem instanceof ModelItem) {

                Model model = proxyHandler.unwrapIfProxy(profiledItem.getModel()) as Model
                if (userSecurityPolicyManager.userCanReadResourceId(profiledItem.getClass(), profiledItem.id, model.getClass(), model.id)) {
                    filteredProfiledItems.add(profiledItem)
                }
            }

        }
        List<Profile> profiles = filteredProfiledItems.collect {createProfileFromEntity(it as D)}
        getKnownMetadataKeys()
            .findAll {key -> (!filter || filter.contains(key))}
            .collectEntries {key ->
                [key, profiles.collect {profile ->
                    profile.getAllFields().find {it.metadataPropertyName == key}.currentValue
                }.toSet()]
            }
    }

    @Override
    ProfileProviderService clone() throws CloneNotSupportedException {
        (getClass().getDeclaredConstructor().newInstance() as ProfileProviderService).tap {
            metadataService = owner.metadataService
            proxyHandler = owner.proxyHandler
            sessionFactory = owner.sessionFactory
        }
    }
}