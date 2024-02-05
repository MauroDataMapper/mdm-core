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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.security.basic.AnonymousUser
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.support.proxy.ProxyHandler
import org.springframework.beans.factory.annotation.Autowired

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

trait MdmDomainService<K extends MdmDomain> implements AnonymisableService{

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    ProxyHandler proxyHandler

    abstract K get(Serializable id)

    abstract List<K> getAll(Collection<UUID> resourceIds)

    abstract List<K> list(Map args)

    abstract Long count()

    abstract void delete(K domain)

    abstract K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier)

    abstract K findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams)

    K save(K domain) {
        // Default behaviours for save in GormEntity
        save(flush: false, validate: true, domain)
    }

    K save(Map args, K domain) {
        domain.save(args)
    }

    K validate(K domain) {
        domain.validate()
        domain
    }

    K unwrapIfProxy(def ge) {
        proxyHandler.unwrapIfProxy(ge) as K
    }

    void delete(Serializable id) {
        K domain = get(id)
        if (domain) delete(domain)
    }

    Class<K> getDomainClass() {
        ParameterizedType parameterizedType = this.getClass().getGenericInterfaces().find {it instanceof ParameterizedType}
        if (!parameterizedType) {
            Type superClassType = this.getClass().getGenericSuperclass()
            parameterizedType = superClassType instanceof ParameterizedType ? superClassType : null
        }
        (Class<K>) parameterizedType?.getActualTypeArguments()[0]
    }

    boolean handles(Class clazz) {
        clazz == getDomainClass()
    }

    boolean handles(String domainType) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, domainType)
        if (!grailsClass) {
            throw new ApiBadRequestException('CISXX', "Unrecognised domain class resource [${domainType}]")
        }
        handles(grailsClass.clazz)
    }

    boolean handlesPathPrefix(String pathPrefix) {
        Class<K> domainClass = getDomainClass()
        domainClass ? (domainClass.getDeclaredConstructor().newInstance() as MdmDomain).pathPrefix == pathPrefix : false
    }

    void anonymise(String createdBy) {
        getDomainClass().findAllByCreatedBy(createdBy).each {domain ->
            domain.createdBy = AnonymousUser.ANONYMOUS_EMAIL_ADDRESS

            // Don't validate because any existing errors in data can cause validations to fail
            domain.save(validate: false)
        }
    }

    boolean handlesModificationPatchOfField(FieldPatchData modificationPatch, MdmDomain targetBeingPatched, K targetDomain, String fieldName) {
        false
    }
}