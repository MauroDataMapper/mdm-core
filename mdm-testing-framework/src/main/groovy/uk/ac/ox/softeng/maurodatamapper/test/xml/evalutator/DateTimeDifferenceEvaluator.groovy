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
package uk.ac.ox.softeng.maurodatamapper.test.xml.evalutator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.DifferenceEvaluator

/**
 * @since 21/02/2017
 */
class DateTimeDifferenceEvaluator implements DifferenceEvaluator {

    Logger logger = LoggerFactory.getLogger(DateTimeDifferenceEvaluator)

    @Override
    ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.EQUAL) return outcome

        Node controlNode = comparison?.controlDetails?.target
        Node testNode = comparison?.testDetails?.target

        if (controlNode && testNode && controlNode.nodeType == Node.TEXT_NODE) {
            Node controlElement = controlNode.parentNode
            Node testElement = testNode.parentNode

            // Check names match for elements containing the values
            if (controlElement.nodeName == testElement.nodeName) {
                if (controlElement?.localName in ['lastUpdated', 'exportedOn', 'dateFinalised']) {
                    return ComparisonResult.EQUAL
                }
            }
        }

        outcome
    }
}
