package uk.ac.ox.softeng.maurodatamapper.test.xml.evalutator

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xmlunit.diff.Comparison
import org.xmlunit.diff.ComparisonResult
import org.xmlunit.diff.ComparisonType
import org.xmlunit.diff.DifferenceEvaluator

/**
 * @since 21/02/2017
 */
class IgnoreOrderDifferenceEvaluator implements DifferenceEvaluator {

    Logger logger = LoggerFactory.getLogger(IgnoreOrderDifferenceEvaluator)

    @Override
    ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
        if (outcome == ComparisonResult.SIMILAR && comparison.type == ComparisonType.CHILD_NODELIST_SEQUENCE) {
            logger.trace('Found similar but nodelist sequence is wrong: {}', comparison)
            return ComparisonResult.EQUAL
        }
        outcome
    }
}
