package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import org.hamcrest.BaseMatcher
import org.hamcrest.Description


/**
 * @since 29/11/2017
 */
class IdMatcher extends BaseMatcher<UUID> {
    @Override
    boolean matches(Object item) {
        if (item instanceof UUID) return true
        if (item instanceof String) {
            try {
                UUID.fromString(item)
                return true
            } catch (Exception ignored) {}
        }
        false

    }

    @Override
    void describeTo(Description description) {
        description.appendText("It must be parseable to UUID")
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText("It must be parseable to UUID")
    }
}
