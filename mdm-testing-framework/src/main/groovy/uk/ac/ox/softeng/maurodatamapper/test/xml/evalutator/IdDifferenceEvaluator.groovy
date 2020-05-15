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
class IdDifferenceEvaluator implements DifferenceEvaluator {

    Logger logger = LoggerFactory.getLogger(IdDifferenceEvaluator)

    @Override
    ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.EQUAL) return outcome

        Node controlNode = comparison?.controlDetails?.target
        Node testNode = comparison?.testDetails?.target

        if (controlNode && testNode && controlNode.nodeType == Node.TEXT_NODE) {
            Node controlElement = controlNode.parentNode
            Node testElement = testNode.parentNode

            // Check names match for elements containing the values
            if (controlElement.nodeName == testElement.nodeName && controlElement.localName == 'id') {
                return ComparisonResult.EQUAL
            }

        }

        outcome
    }
}
