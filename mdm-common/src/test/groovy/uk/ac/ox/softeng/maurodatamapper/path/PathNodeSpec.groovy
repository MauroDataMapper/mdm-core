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
package uk.ac.ox.softeng.maurodatamapper.path

import spock.lang.Specification

/**
 * @since 20/07/2021
 */
class PathNodeSpec extends Specification {

    void 'test path node creation'() {
        given:
        PathNode pathNode = new PathNode('dm', 'test model', true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        !pathNode.modelIdentifier
        !pathNode.attribute
        pathNode.toString() == 'dm:test model'
    }

    void 'test path node creation with parsing'() {
        given:
        PathNode pathNode = new PathNode('dm:test model', true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        !pathNode.modelIdentifier
        !pathNode.attribute
    }

    void 'test path node creation with parsing with model'() {
        given:
        PathNode pathNode = new PathNode('dm:test model$main', true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        pathNode.modelIdentifier == 'main'
        !pathNode.attribute
        pathNode.toString() == 'dm:test model$main'
    }

    void 'test path node creation with parsing with attribute'() {
        given:
        PathNode pathNode = new PathNode('dm:test model@description', true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        !pathNode.modelIdentifier
        pathNode.attribute == 'description'
        pathNode.isPropertyNode()
        pathNode.toString() == 'dm:test model@description'
    }

    void 'test path node creation with parsing with model and attribute'() {
        given:
        PathNode pathNode = new PathNode('dm:test model$main@description', true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        pathNode.modelIdentifier == 'main'
        pathNode.attribute == 'description'
        pathNode.toString() == 'dm:test model$main@description'
    }

    void 'test path node matching on identifier and model identifier'() {
        when: 'same branch names'
        PathNode pathNode1 = new PathNode('dm:test model$main', true)
        PathNode pathNode2 = new PathNode('dm:test model$main', true)

        then: 'matches'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'defaulting branch name means we dont care about it'
        pathNode2 = new PathNode('dm:test model', true)

        then: 'matches but does not equal'
        pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'different branch names'
        pathNode2 = new PathNode('dm:test model$test', true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'same branch name different identifier'
        pathNode2 = new PathNode('dm:test model 2$main', true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'versionable model identifier vs branch name'
        pathNode2 = new PathNode('dm:test model$1.0.0', true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1.0.0', true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1.0', true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1', true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'different identifiers and different prefix'
        pathNode2 = new PathNode('dc:test class', true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2
    }

    void 'test path node matching with attributes'() {
        when: 'same branch names'
        PathNode pathNode1 = new PathNode('dm:test model$main', true)
        PathNode pathNode2 = new PathNode('dm:test model$main', true)

        then: 'matches'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'attribute included'
        pathNode2 = new PathNode('dm:test model$main@description', true)

        then: 'matches but does not equal'
        pathNode1.matches(pathNode2)
        pathNode1 != pathNode2
    }
}
