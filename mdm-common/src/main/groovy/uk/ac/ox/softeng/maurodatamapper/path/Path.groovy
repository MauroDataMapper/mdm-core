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
package uk.ac.ox.softeng.maurodatamapper.path

import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

/**
 * @since 28/08/2020
 */
class Path {

    //Need to escape the vertical bar which we are using as the split delimiter
    static String PATH_DELIMITER = '\\|'

    //Arbitrary maximum number of nodes, to avoid unexpectedly long iteration
    static int MAX_NODES = 10

    List<PathNode> pathNodes

    private Path() {
        pathNodes = []
    }

    /*
     * Make a list of PathNode from the provided path string. The path string is like dc:class-label|de:element-label
     * which means 'The DataElement labelled element-label which belongs to the DataClass labelled class-label which
     * belongs to the current DataModel'
     * @param path The path
     */

    private Path(String path) {
        this()

        if (path) {
            String[] splits = path.split(PATH_DELIMITER, MAX_NODES)
            int lastIndex = splits.size() - 1

            splits.eachWithIndex {String node, int i ->
                pathNodes << new PathNode(node, i == lastIndex)
            }
        }
    }

    int size() {
        pathNodes.size()
    }

    PathNode getAt(int i) {
        pathNodes[i]
    }

    PathNode last() {
        pathNodes.last()
    }

    PathNode first() {
        pathNodes.first()
    }

    Path addToPathNodes(PathNode pathNode) {
        pathNodes.add(pathNode)
        this
    }

    Path addToPathNodes(String prefix, String pathIdentifier, boolean isLast) {
        addToPathNodes(new PathNode(prefix, pathIdentifier, isLast))
    }

    Path getParent() {
        clone().tap {
            pathNodes.removeLast()
        }
    }

    boolean isAbsoluteTo(CreatorAware creatorAware, String modelIdentifierOverride = null) {
        // If the first node in this path matches the supplied object then this path is absolute against the supplied object,
        // otherwise it may be relative or may not be inside this object
        Path rootPath = from(creatorAware)
        isAbsoluteTo(rootPath, modelIdentifierOverride)
    }

    boolean isAbsoluteTo(Path rootPath, String modelIdentifierOverride = null) {
        // If the first node in this path matches the supplied object then this path is absolute against the supplied object,
        // otherwise it may be relative or may not be inside this object
        rootPath.first().matches(this.first(), modelIdentifierOverride)
    }

    boolean isEmpty() {
        pathNodes.isEmpty()
    }

    void each(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.path.PathNode') Closure closure) {
        pathNodes.each closure
    }

    PathNode find(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.path.PathNode') Closure closure) {
        pathNodes.find closure
    }

    Path getChildPath() {
        clone().tap {
            pathNodes.removeAt(0)
        }
    }

    String toString() {
        pathNodes.join('|')
    }

    Path clone() {
        Path local = this
        new Path().tap {
            pathNodes = local.pathNodes.collect {it.clone()}
        }
    }

    boolean matches(Path otherPath) {
        if (size() != otherPath.size()) return false
        for (i in 0..<size()) {
            if (!this[i].matches(otherPath[i])) return false
        }
        true
    }

    static Path from(String path) {
        new Path(path)
    }

    static Path from(PathNode pathNode) {
        new Path().tap {
            pathNodes << pathNode
        }
    }

    static Path from(String prefix, String pathIdentifier) {
        new Path().tap {
            pathNodes << new PathNode(prefix, pathIdentifier, true)
        }
    }

    static Path from(Path parentPath, String prefix, String pathIdentifier) {
        parentPath ? parentPath.clone().addToPathNodes(prefix, pathIdentifier, false) : from(prefix, pathIdentifier)
    }

    static Path from(String parentPath, String prefix, String pathIdentifier) {
        from(from(parentPath), prefix, pathIdentifier)
    }

    static Path from(CreatorAware... domains) {
        new Path().tap {
            domains.eachWithIndex {CreatorAware domain, int i ->
                pathNodes << new PathNode(domain.pathPrefix, domain.pathIdentifier, false)
            }
        }
    }

    static Path from(List<CreatorAware> domains) {
        new Path().tap {
            domains.eachWithIndex {CreatorAware domain, int i ->
                pathNodes << new PathNode(domain.pathPrefix, domain.pathIdentifier, false)
            }
        }
    }

    static Path forAttributeOnPath(Path path, String attribute) {
        Path attributePath = path.clone()
        attributePath.last().attribute = attribute
        attributePath
    }
}
