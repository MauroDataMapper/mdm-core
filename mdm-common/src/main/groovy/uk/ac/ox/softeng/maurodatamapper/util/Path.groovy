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

/**
 * @since 28/08/2020
 */
class Path {

    //Need to escape the vertical bar which we are using as the split delimiter
    static String PATH_DELIMITER = "\\|"

    //Arbitrary maximum number of nodes, to avoid unexpectedly long iteration
    static int MAX_NODES = 10

    List<PathNode> pathNodes

    /*
     * Make a list of PathNode from the provided path string. The path string is like dm:|dc:class-label|de:element-label
     * which means 'The DataElement labelled element-label which belongs to the DataClass labelled class-label which
     * belongs to the current DataModel'
     * @param path The path
     */
    Path (String path) {
        pathNodes = []

        String[] splits = path.split(PATH_DELIMITER, MAX_NODES)

        for (String s: splits) {
            pathNodes << new PathNode(s)
        }
    }

}
