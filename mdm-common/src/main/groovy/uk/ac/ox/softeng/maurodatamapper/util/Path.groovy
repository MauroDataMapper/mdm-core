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
package uk.ac.ox.softeng.maurodatamapper.util

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

    Path() {

    }

    /*
     * Make a list of PathNode from the provided path string. The path string is like dm:|dc:class-label|de:element-label
     * which means 'The DataElement labelled element-label which belongs to the DataClass labelled class-label which
     * belongs to the current DataModel'
     * @param path The path
     */

    Path(String path) {
        pathNodes = []

        if (path) {
            String[] splits = path.split(PATH_DELIMITER, MAX_NODES)

            for (String s : splits) {
                pathNodes << new PathNode(s)
            }
        }
    }

    int getSize() {
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

    boolean isEmpty() {
        pathNodes.isEmpty()
    }

    void each(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.util.PathNode') Closure closure) {
        pathNodes.each closure
    }

    Path getChildPath() {
        new Path(pathNodes: pathNodes[1..size - 1])
    }

    String toString() {
        pathNodes.join('|')
    }

    static Path from(String path) {
        new Path(path)
    }

    static Path from(String prefix, String pathIdentifier) {
        new Path().tap {
            pathNodes = [new PathNode(prefix, pathIdentifier)]
        }
    }

    static Path from(CreatorAware... domains) {
        new Path().tap {
            pathNodes = domains.collect {new PathNode(it.pathPrefix, it.pathIdentifier)}
        }
    }
}
