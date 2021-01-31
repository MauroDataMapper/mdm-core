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
package uk.ac.ox.softeng.maurodatamapper.test.xml

import uk.ac.ox.softeng.maurodatamapper.test.xml.converter.JavaLocalDateConverter
import uk.ac.ox.softeng.maurodatamapper.test.xml.converter.JavaOffsetDateTimeConverter

import org.w3c.dom.CDATASection
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.xmlunit.diff.ElementSelector
import org.xmlunit.util.IterableNodeList

import static org.xmlunit.diff.ElementSelectors.byName

/**
 * @since 27/02/2017
 */
class Utils {

    /**
     * Elements with the same local name (and namespace URI - if any)
     * and nested text (if any) can be compared.
     */
    public static final ElementSelector byNameAndResolvedText = new ElementSelector() {
        @Override
        boolean canBeCompared(Element controlElement,
                              Element testElement) {
            return byName.canBeCompared(controlElement, testElement) &&
                   bothNullOrEqual(getSingleLevelMergedNestedText(controlElement),
                                   getSingleLevelMergedNestedText(testElement))
        }
    }


    static Object getMergedNestedText(Node element) {
        StringBuilder sb = new StringBuilder()
        if (element) {
            for (Node child : new IterableNodeList(element.getChildNodes())) {
                if (child instanceof Text || child instanceof CDATASection) {
                    String s = child.getNodeValue()
                    if (s != null) {
                        sb.append(s)
                    }
                } else {
                    sb.append(getMergedNestedText(child))
                }
            }
        }
        convert(sb.toString())
    }

    static Object convert(String s) {
        if (!s) return s

        // Try to convert to a number
        if (s.isNumber()) return s.toBigDecimal()

        // Try offset date time
        try {
            return new JavaOffsetDateTimeConverter().convert(s)
        } catch (Exception ignored) {}

        // Try local date
        try {
            return new JavaLocalDateConverter().convert(s)
        } catch (Exception ignored) {}

        // Just return s
        s
    }

    static boolean bothNullOrEqual(Object o1, Object o2) {
        o1 == null ? o2 == null : o1 == o2
    }

    static Element getDirectChildNode(Element parent, String childLocalName) {
        parent.childNodes.find {it.localName == childLocalName} as Element
    }

    static Object getSingleLevelMergedNestedText(Node n) {
        StringBuilder sb = new StringBuilder()
        if (n) {
            for (Node child : new IterableNodeList(n.getChildNodes())) {
                if (child instanceof Text || child instanceof CDATASection) {
                    String s = child.getNodeValue()
                    if (s != null) {
                        sb.append(s)
                    }
                }
            }
        }
        convert(sb.toString())
    }

}
