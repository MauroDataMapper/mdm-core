package uk.ac.ox.softeng.maurodatamapper.test.xml

import uk.ac.ox.softeng.maurodatamapper.test.xml.evalutator.DateTimeDifferenceEvaluator
import uk.ac.ox.softeng.maurodatamapper.test.xml.evalutator.IdDifferenceEvaluator
import uk.ac.ox.softeng.maurodatamapper.test.xml.evalutator.IgnoreOrderDifferenceEvaluator
import uk.ac.ox.softeng.maurodatamapper.test.xml.selector.CustomElementSelector

import groovy.util.logging.Slf4j
import groovy.xml.XmlUtil
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.Diff
import org.xmlunit.diff.DifferenceEvaluator
import org.xmlunit.diff.DifferenceEvaluators
import org.xmlunit.diff.ElementSelector
import org.xmlunit.input.CommentLessSource

import javax.xml.transform.Source

import static org.junit.Assert.assertTrue

/**
 * @since 18/07/2016
 */
@Slf4j
trait XmlComparer {

    String failureReason

    void completeCompareXml(String expected, String actual) {
        def xmlMatchesSubmitted = compareXml(expected, actual)
        assertTrue failureReason, xmlMatchesSubmitted
    }

    ElementSelector getElementSelector() {
        new CustomElementSelector()
    }

    boolean compareXml(String expected, String actual) {

        Diff myDiffIdentical = DiffBuilder
            .compare(getCommentLess(expected))
            .withTest(getCommentLess(actual))
            .normalizeWhitespace().ignoreWhitespace()
            .withNodeMatcher(new DefaultNodeMatcher(getElementSelector()))
            .withDifferenceEvaluator(getDifferenceEvaluator())
            .checkForIdentical()
            .build()
        if (myDiffIdentical.hasDifferences()) {
            log.error('\n----------------------------------- expected -----------------------------------\n{}', prettyPrint(expected))
            log.error('\n----------------------------------- actual   -----------------------------------\n{}', prettyPrint(actual))
            failureReason = myDiffIdentical.toString()
            log.error(failureReason)
        }
        !myDiffIdentical.hasDifferences()
    }

    DifferenceEvaluator getDifferenceEvaluator() {
        DifferenceEvaluators.chain(
            DifferenceEvaluators.Default,
            new IdDifferenceEvaluator(),
            new DateTimeDifferenceEvaluator(),
            new IgnoreOrderDifferenceEvaluator(),
            )
    }

    Source getSource(Object object) {
        Input.from(object).build()
    }

    Source getCommentLess(String xml) {
        new CommentLessSource(getSource(xml))
    }

    String prettyPrint(String xml) {
        try {
            XmlUtil.serialize(new XmlParser().parseText(xml))
        } catch (Exception ignored) {
            xml
        }
    }
}