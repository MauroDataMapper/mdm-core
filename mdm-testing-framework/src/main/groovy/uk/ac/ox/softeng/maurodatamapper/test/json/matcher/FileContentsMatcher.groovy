package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import org.hamcrest.BaseMatcher
import org.hamcrest.Description


/**
 * @since 29/11/2017
 */
class FileContentsMatcher extends BaseMatcher<UUID> {
    @Override
    boolean matches(Object item) {
        if (item instanceof List) {
            return item.every {it instanceof Number}
        }
        false

    }

    @Override
    void describeTo(Description description) {
        description.appendText("It must be be a non-empty byte array")
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText("It must be be a non-empty byte array")
    }
}
