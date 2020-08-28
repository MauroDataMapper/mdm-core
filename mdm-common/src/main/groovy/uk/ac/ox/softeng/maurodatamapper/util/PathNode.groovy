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
class PathNode {

    static String NODE_DELIMITER = ":"

    //Ignore any unexpected delimiters
    static int MAX_PIECES =  2

    String typePrefix
    String label

    PathNode (String node) {
        String[] splits = node.split(NODE_DELIMITER, MAX_PIECES)

        if (splits.length >= 1) {
            typePrefix = splits[0]
        }

        if (splits.length >= 2) {
            label = splits[1]
        }
    }

}
