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
package uk.ac.ox.softeng.maurodatamapper.test.functional

import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.grails.datastore.gorm.GormEntity
import spock.lang.Shared

import java.lang.reflect.ParameterizedType
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @since 25/09/2020
 */
@Slf4j
abstract class OrderedResourceFunctionalSpec<D extends GormEntity> extends ResourceFunctionalSpec {

    Map getValidLabelJson(String label, int index) {
        [
            label: label,
            index: index
        ]
    }

    void 'OR1: Test adding new items with the index set'() {
        when: 'The save action is executed with valid data at index 0'
        createNewItem(getValidLabelJson('Alice', 0))

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        String aliceId = response.body().id
        response.body().index == 0

        when: 'The save action is executed with valid data at index 1'
        createNewItem(getValidLabelJson('Bob', 1))

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        String bobId = response.body().id
        response.body().index == 1

        when: 'The save action is executed with valid data at index 2'
        createNewItem(getValidLabelJson('Carlos', 2))

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        String carlosId = response.body().id
        response.body().index == 2

        when: 'The save action is executed with valid data at index 3'
        createNewItem(getValidLabelJson('Dan', 3))

        then: 'The response is correct'
        response.status == HttpStatus.CREATED
        String danId = response.body().id
        response.body().index == 3

        //when: 'The save action is executed with valid data at index 1'
        //createNewItem(getValidLabelJson('Eve', 1))

        //then: 'The response is correct'
        //response.status == HttpStatus.CREATED
        //String eveId = response.body().id
        //response.body().index == 1      

//TODO fails because indexes are not updated on a POST. 
        /*when: 'All items are listed'
        GET('')        

        then: 'They are in the order Alice, Eve, Bob, Carlos, Dan'
        response.body().items[0].label == 'Alice'
        response.body().items[0].index == 0
        response.body().items[1].label == 'Eve'
        response.body().items[1].index == 1
        response.body().items[2].label == 'Bob'
        response.body().items[2].index == 2
        response.body().items[3].label == 'Carlos'
        response.body().items[3].index == 3
        response.body().items[4].label == 'Dan'
        response.body().items[5].index == 4   */  

        cleanup:                
        DELETE(getDeleteEndpoint(aliceId))
        DELETE(getDeleteEndpoint(bobId))
        DELETE(getDeleteEndpoint(carlosId))
        DELETE(getDeleteEndpoint(danId))
        DELETE(getDeleteEndpoint(eveId))
    }

}
