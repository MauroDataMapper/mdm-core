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

    Map getValidLabelJson(String label, int index = -1) {
        if (index >= 0) {
            [label: label, index: index]
        } else {
            [label: label]
        }
    }

   void 'OR1: Test ordering on update when the index was specified on insert'() {
        given: 'Five resources with specified indices'
        String aId = createNewItem(getValidLabelJson('A', 0))
        String bId = createNewItem(getValidLabelJson('B', 1))
        String cId = createNewItem(getValidLabelJson('C', 2))
        String dId = createNewItem(getValidLabelJson('D', 3))
        String eId = createNewItem(getValidLabelJson('E', 4))    

        when: 'All items are listed'
        GET('')

        then: 'They are in the order A, B, C, D, E'
        response.body().items[0].label == 'A'
        response.body().items[0].index == 0
        response.body().items[1].label == 'B'
        response.body().items[1].index == 1
        response.body().items[2].label == 'C'
        response.body().items[2].index == 2
        response.body().items[3].label == 'D'
        response.body().items[3].index == 3
        response.body().items[4].label == 'E'
        response.body().items[4].index == 4

        when: 'Item E is PUT at the top of the list'
        PUT(eId, getValidLabelJson('E', 0))

        then: 'The PUT works'
        response.status == HttpStatus.OK
        response.body().index == 0
        String fId = response.body().id

        when: 'All items are listed'
        GET('')

        then: 'They are in the order E, A, B, C, D'
        response.body().items[0].label == 'E'
        response.body().items[0].index == 0
        response.body().items[1].label == 'A'
        response.body().items[1].index == 1
        response.body().items[2].label == 'B'
        response.body().items[2].index == 2
        response.body().items[3].label == 'C'
        response.body().items[3].index == 3
        response.body().items[4].label == 'D'
        response.body().items[4].index == 4

        when: 'Item E is PUT to the  middle of the list'
        PUT(eId, getValidLabelJson('E', 2))

        then: 'The PUT works'
        response.status == HttpStatus.OK
        response.body().index == 2

        when: 'All items are listed'
        GET('')

        then: 'They are in the order A, B, E, C, D'
        response.body().items[0].label == 'A'
        response.body().items[0].index == 0
        response.body().items[1].label == 'B'
        response.body().items[1].index == 1
        response.body().items[2].label == 'E'
        response.body().items[2].index == 2
        response.body().items[3].label == 'C'
        response.body().items[3].index == 3
        response.body().items[4].label == 'D'
        response.body().items[4].index == 4           

        when: 'Item E is PUT to the  end of the list'
        PUT(eId, getValidLabelJson('E', 4))

        then: 'The PUT works'
        response.status == HttpStatus.OK
        response.body().index == 4

        when: 'All items are listed'
        GET('')

        then: 'They are in the order A, B, C, D, E'
        response.body().items[0].label == 'A'
        response.body().items[0].index == 0
        response.body().items[1].label == 'B'
        response.body().items[1].index == 1
        response.body().items[2].label == 'C'
        response.body().items[2].index == 2
        response.body().items[3].label == 'D'
        response.body().items[3].index == 3
        response.body().items[4].label == 'E'
        response.body().items[4].index == 4           

    }

   void 'OR2: Test ordering on insert when the index was specified on insert'() {
        given: 'Five resources with specified indices'
        String aId = createNewItem(getValidLabelJson('A', 0))
        String bId = createNewItem(getValidLabelJson('B', 1))
        String cId = createNewItem(getValidLabelJson('C', 2))
        String dId = createNewItem(getValidLabelJson('D', 3))
        String eId = createNewItem(getValidLabelJson('E', 4))    
     

        when: 'All items are listed'
        GET('')

        then: 'They are in the order A, B, C, D, E'
        response.body().items[0].label == 'A'
        response.body().items[0].index == 0
        response.body().items[1].label == 'B'
        response.body().items[1].index == 1
        response.body().items[2].label == 'C'
        response.body().items[2].index == 2
        response.body().items[3].label == 'D'
        response.body().items[3].index == 3
        response.body().items[4].label == 'E'
        response.body().items[4].index == 4

        when: 'Item F is POSTed at the top of the list'
        POST('', getValidLabelJson('F', 0))
        
        then: 'The item is created'
        response.status == HttpStatus.CREATED
        response.body().index == 0
        String fId = response.body().id

        when: 'All items are listed'
        GET('')

        then: 'They are in the order F, A, B, C, D, E'
        response.body().items[0].label == 'F'
        response.body().items[0].index == 0
        response.body().items[1].label == 'A'
        response.body().items[1].index == 1
        response.body().items[2].label == 'B'
        response.body().items[2].index == 2
        response.body().items[3].label == 'C'
        response.body().items[3].index == 3
        response.body().items[4].label == 'D'
        response.body().items[4].index == 4   
        response.body().items[5].label == 'E'
        response.body().items[5].index == 5               

        when: 'Item G is POSTed in the middle of the list'
        POST('', getValidLabelJson('G', 2))
        
        then: 'The item is created'
        response.status == HttpStatus.CREATED
        response.body().index == 2
        String gId = response.body().id

        when: 'All items are listed'
        GET('')

        then: 'They are in the order F, A, G, B, C, D, E'
        response.body().items[0].label == 'F'
        response.body().items[0].index == 0
        response.body().items[1].label == 'A'
        response.body().items[1].index == 1
        response.body().items[2].label == 'G'
        response.body().items[2].index == 2
        response.body().items[3].label == 'B'
        response.body().items[3].index == 3
        response.body().items[4].label == 'C'
        response.body().items[4].index == 4   
        response.body().items[5].label == 'D'
        response.body().items[5].index == 5   
        response.body().items[6].label == 'E'
        response.body().items[6].index == 6         
        
    }

   void 'OR3: Test ordering on update when the index was not specified on insert'() {
        given: 'Five resources without specified indices'
        String cId = createNewItem(getValidLabelJson('C'))
        String dId = createNewItem(getValidLabelJson('D'))
        String aId = createNewItem(getValidLabelJson('A'))
        String eId = createNewItem(getValidLabelJson('E'))
        String bId = createNewItem(getValidLabelJson('B'))
        
        when: 'All items are listed'
        GET('')

        then: 'They are in the order A, B, C, D, E because label sorting is used'
        response.body().items[0].label == 'A'
        !response.body().items[0].index
        response.body().items[1].label == 'B'
        !response.body().items[1].index
        response.body().items[2].label == 'C'
        !response.body().items[2].index
        response.body().items[3].label == 'D'
        !response.body().items[3].index
        response.body().items[4].label == 'E'
        !response.body().items[4].index

        when: 'Item F is POSTed at the top of the list'
        POST('', getValidLabelJson('F', 0))
        
        then: 'The item is created'
        response.status == HttpStatus.CREATED
        response.body().index == 0
        String fId = response.body().id

        when: 'All items are listed'
        GET('')

        then: 'They are in the order F, A, B, C, D, E'
        response.body().items[0].label == 'F'
        response.body().items[0].index == 0
        response.body().items[1].label == 'A'
        !response.body().items[1].index
        response.body().items[2].label == 'B'
        !response.body().items[2].index
        response.body().items[3].label == 'C'
        !response.body().items[3].index
        response.body().items[4].label == 'D'
        !response.body().items[4].index 
        response.body().items[5].label == 'E'
        !response.body().items[5].index

        when: 'Item C is PUT with an index of 1'
        PUT(cId, getValidLabelJson('C', 1))

        then: 'The item is updated'
        response.status == HttpStatus.OK
        response.body().index == 1

        when: 'All items are listed'
        GET('')

        then: 'They are in the order F, C, A, B, D, E'
        response.body().items[0].label == 'F'
        response.body().items[0].index == 0
        response.body().items[1].label == 'C'
        response.body().items[1].index == 1
        response.body().items[2].label == 'A'
        !response.body().items[2].index
        response.body().items[3].label == 'B'
        !response.body().items[3].index
        response.body().items[4].label == 'D'
        !response.body().items[4].index  
        response.body().items[5].label == 'E'
        !response.body().items[5].index          

        when: 'Item B is PUT with an index of 1'
        PUT(bId, getValidLabelJson('B', 1))

        then: 'The item is updated'
        response.status == HttpStatus.OK
        response.body().index == 1

        when: 'All items are listed'
        GET('')

        then: 'They are in the order F, B, C, A, D, E'
        response.body().items[0].label == 'F'
        response.body().items[0].index == 0
        response.body().items[1].label == 'B'
        response.body().items[1].index == 1
        response.body().items[2].label == 'C'
        response.body().items[2].index == 2
        response.body().items[3].label == 'A'
        !response.body().items[3].index
        response.body().items[4].label == 'D'
        !response.body().items[4].index 
        response.body().items[5].label == 'E'
        !response.body().items[5].index              
   }

   void 'OR4: Test ordering on update when moving an item from the top to bottom'() {
        given: 'Three resources with indices 0, 1 and 2'
        String aId = createNewItem(getValidLabelJson('emptyclass', 0))
        String bId = createNewItem(getValidLabelJson('parent', 1))
        String cId = createNewItem(getValidLabelJson('content', 2))
        
        when: 'All items are listed'
        GET('')

        then: 'They are in the order emptyclass, parent, content'
        response.body().items[0].label == 'emptyclass'
        response.body().items[0].index == 0
        response.body().items[1].label == 'parent'
        response.body().items[1].index == 1
        response.body().items[2].label == 'content'
        response.body().items[2].index == 2

        when: 'emptyclass is PUT at the bottom of the list'
        PUT(aId, getValidLabelJson('emptyclass', 2))
        
        then: 'The item is updated'
        response.status == HttpStatus.OK
        response.body().index == 2

        when: 'All items are listed'
        GET('')

        then: 'They are in the order parent, content, emptyclass'
        response.body().items[0].label == 'parent'
        response.body().items[0].index == 0
        response.body().items[1].label == 'content'
        response.body().items[1].index == 1
        response.body().items[2].label == 'emptyclass'
        response.body().items[2].index == 2
   }
}
