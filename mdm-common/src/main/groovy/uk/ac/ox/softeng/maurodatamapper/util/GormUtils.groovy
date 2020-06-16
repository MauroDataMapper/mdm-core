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
package uk.ac.ox.softeng.maurodatamapper.util

import grails.validation.Validateable
import grails.validation.ValidationException
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.springframework.context.MessageSource

@Slf4j
class GormUtils {

    static void check(MessageSource messageSource, GormEntity domainObj) throws ValidationException {
        if (!domainObj) throw new ValidationException('No domain object to save', null)

        boolean valid = domainObj.validate()

        if (!valid) {
            outputDomainErrors(messageSource, domainObj)
            throw new ValidationException("Domain object is not valid. Has ${domainObj.errors.errorCount} errors", domainObj.errors)
        }
        null
    }

    static void check(MessageSource messageSource, Validateable domainObj) throws ValidationException {
        if (!domainObj) throw new ValidationException('No domain object to save', null)

        boolean valid = domainObj.validate()

        if (!valid) {
            outputDomainErrors(messageSource, domainObj)
            throw new ValidationException("Domain object is not valid. Has ${domainObj.errors.errorCount} errors", domainObj.errors)
        }
        null
    }

    static void checkAndSave(MessageSource messageSource, GormEntity domainObj) throws ValidationException {
        check(messageSource, domainObj)
        save(domainObj)
    }

    static void checkAndSave(MessageSource messageSource, GormEntity... domainObjs) throws ValidationException {
        domainObjs.each {checkAndSave(messageSource, it,)}
    }

    static def save(GormEntity domainObj) {
        domainObj.save(failOnError: true, validate: false, flush: true) ? true : false
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    static def outputDomainErrors(MessageSource messageSource, def domainObj) {
        log.error 'Errors validating domain: {}', domainObj.class.simpleName
        System.err.println 'Errors validating domain: ' + domainObj.class.simpleName
        domainObj.errors.allErrors.each {error ->

            String msg = messageSource ? messageSource.getMessage(error, Locale.default) :
                         "${error.defaultMessage} :: ${Arrays.asList(error.arguments)}"

            log.error msg
            System.err.println msg
        }
    }
}
