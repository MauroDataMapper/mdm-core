package uk.ac.ox.softeng.maurodatamapper.path

import spock.lang.Specification

/**
 * @since 20/07/2021
 */
class PathNodeSpec extends Specification {

    void 'test path node creation'() {
        given:
        PathNode pathNode = new PathNode('dm', 'test model', true, true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        !pathNode.modelIdentifier
        !pathNode.attribute
        pathNode.toString() == 'dm:test model'
    }

    void 'test path node creation with parsing'() {
        given:
        PathNode pathNode = new PathNode('dm:test model', true, true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        !pathNode.modelIdentifier
        !pathNode.attribute
    }

    void 'test path node creation with parsing with model'() {
        given:
        PathNode pathNode = new PathNode('dm:test model$main', true, true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        pathNode.modelIdentifier == 'main'
        !pathNode.attribute
        pathNode.toString() == 'dm:test model$main'
    }

    void 'test path node creation with parsing with attribute'() {
        given:
        PathNode pathNode = new PathNode('dm:test model@description', true, true)

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
        PathNode pathNode = new PathNode('dm:test model$main@description', true, true)

        expect:
        pathNode.prefix == 'dm'
        pathNode.identifier == 'test model'
        pathNode.modelIdentifier == 'main'
        pathNode.attribute == 'description'
        pathNode.toString() == 'dm:test model$main@description'
    }

    void 'test path node matching on identifier and model identifier'() {
        when: 'same branch names'
        PathNode pathNode1 = new PathNode('dm:test model$main', true, true)
        PathNode pathNode2 = new PathNode('dm:test model$main', true, true)

        then: 'matches'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'defaulting branch name means we dont care about it'
        pathNode2 = new PathNode('dm:test model', true, true)

        then: 'matches but does not equal'
        pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'different branch names'
        pathNode2 = new PathNode('dm:test model$test', true, true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'same branch name different identifier'
        pathNode2 = new PathNode('dm:test model 2$main', true, true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'versionable model identifier vs branch name'
        pathNode2 = new PathNode('dm:test model$1.0.0', true, true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1.0.0', true, true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1.0', true, true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'same versionable model identifier'
        pathNode1 = new PathNode('dm:test model$1', true, true)

        then: 'match and equality'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'different identifiers and different prefix'
        pathNode2 = new PathNode('dc:test class', true, true)

        then: 'no match'
        !pathNode1.matches(pathNode2)
        pathNode1 != pathNode2
    }

    void 'test path node matching with attributes'() {
        when: 'same branch names'
        PathNode pathNode1 = new PathNode('dm:test model$main', true, true)
        PathNode pathNode2 = new PathNode('dm:test model$main', true, true)

        then: 'matches'
        pathNode1.matches(pathNode2)
        pathNode1 == pathNode2

        when: 'attribute included'
        pathNode2 = new PathNode('dm:test model$main@description', true, true)

        then: 'matches but does not equal'
        pathNode1.matches(pathNode2)
        pathNode1 != pathNode2
    }
}
