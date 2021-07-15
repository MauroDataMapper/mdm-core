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

import groovy.util.logging.Slf4j

/**
 * @since 28/08/2020
 */
@Slf4j
class PathNode {

    static String NODE_DELIMITER = ":"

    String typePrefix
    String label

    PathNode(String prefix, String label) {
        this.typePrefix = prefix
        this.label = label
    }

    /*
    Parse a string into a type prefix and label.
    The string is imagined to be of the format tp:label
    Label can contain a : character, so some real examples are
    dm:my-data-model (type prefix = dm, label = my-data-model)
    te:my-code:my-definition (type prefix = te, label = my-code:my-definition)
     */

    PathNode(String node) {
        //Look for the first :
        int index = node.indexOf(NODE_DELIMITER)

        //If there is a : not in the zero index then extract the type prefix
        if (index > 0) {
            typePrefix = node.substring(0, index)
        }

        //If there are characters after the : then extract these as the label
        if (index < node.length() - 1) {
            label = node.substring(index + 1)
        }
    }

    boolean hasTypePrefix() {
        typePrefix
    }

    String toString() {
        if (typePrefix && label) return "${typePrefix}:${label}"
        if (typePrefix) return typePrefix
        label
    }

    boolean matches(PathNode pathNode) {
        matches(pathNode.typePrefix, pathNode.label)
    }

    boolean matches(CreatorAware creatorAware) {
        matches(creatorAware.pathPrefix, creatorAware.pathIdentifier)
    }

    boolean matches(String pathPrefix, String pathIdentifier) {
        matchesLabel(pathIdentifier) && matchesPrefix(pathPrefix)
    }

    boolean matchesLabel(String pathIdentifier) {

        // No label suggests we're at the top of the path and we've come in via an id tracking path
        // confirmation will come via the prefix check
        if (!label) return true

        if (label == pathIdentifier) return true

        // Allow for the possibility of a label which has been defaulted (models can be path'd without the branch or version)
        String[] pathIdentifierSplit = pathIdentifier.split(/:/)
        if (label == pathIdentifierSplit[0]) return true

        // Some of the legacy paths included : so we handle submissions of this format
        String[] labelSplit = label.split(/:/)
        if (labelSplit[0] == pathIdentifier) return true
        log.warn("Resource identifier [{}] does not match the path node [{}]", pathIdentifier, this)
        false
    }

    boolean matchesPrefix(String pathPrefix) {
        if (pathPrefix != typePrefix) {
            log.warn("Resource prefix [{}] does not match the path node [{}]", typePrefix, this)
            return false
        }
        true
    }

    PathNode clone() {
        new PathNode(this.typePrefix, this.label)
    }
}
