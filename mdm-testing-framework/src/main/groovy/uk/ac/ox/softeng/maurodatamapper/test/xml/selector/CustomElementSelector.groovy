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
package uk.ac.ox.softeng.maurodatamapper.test.xml.selector

import org.w3c.dom.Element
import org.xmlunit.diff.ElementSelector
import org.xmlunit.diff.ElementSelectors

import static uk.ac.ox.softeng.maurodatamapper.test.xml.Utils.byNameAndResolvedText
import static uk.ac.ox.softeng.maurodatamapper.test.xml.Utils.getDirectChildNode

/**
 * @since 24/02/2017
 */
class CustomElementSelector implements ElementSelector {

    @Override
    boolean canBeCompared(Element controlElement, Element testElement) {
        Element controlChild
        Element testChild

        if (controlElement.localName == 'enumerationValue') {
            controlChild = getDirectChildNode(controlElement, 'key')
            testChild = getDirectChildNode(testElement, 'key')
            if (controlChild && testChild) {
                return byNameAndResolvedText.canBeCompared(controlChild, testChild)
            }


        } else if (controlElement.localName == 'metadata') {
            Element controlChildNs = getDirectChildNode(controlElement, 'namespace')
            Element testChildNs = getDirectChildNode(testElement, 'namespace')
            Element controlChildKey = getDirectChildNode(controlElement, 'key')
            Element testChildKey = getDirectChildNode(testElement, 'key')
            if (controlChildNs && controlChildKey && testChildNs && testChildKey) {
                return byNameAndResolvedText.canBeCompared(controlChildNs, testChildNs) &&
                       byNameAndResolvedText.canBeCompared(controlChildKey, testChildKey)
            }
        }

        if (controlElement.hasChildNodes()) {
            controlChild = getDirectChildNode(controlElement, 'label')
            testChild = getDirectChildNode(testElement, 'label')
            if (controlChild && testChild) {
                return byNameAndResolvedText.canBeCompared(controlChild, testChild)
            }

            controlChild = getDirectChildNode(controlElement, 'code')
            testChild = getDirectChildNode(testElement, 'code')
            if (controlChild && testChild) {
                return byNameAndResolvedText.canBeCompared(controlChild, testChild)
            }

        }
        // Fall back on byNameAndResolvedText
        ElementSelectors.byName.canBeCompared(controlElement, testElement)
    }

}

