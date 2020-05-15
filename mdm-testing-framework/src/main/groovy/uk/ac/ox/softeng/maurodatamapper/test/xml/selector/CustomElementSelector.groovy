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

