package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import org.hamcrest.BaseMatcher
import org.hamcrest.Description

import java.time.OffsetDateTime

/**
 * @since 20/11/2017
 */
class OffsetDateTimeMatcher extends BaseMatcher<OffsetDateTime> {
    @Override
    boolean matches(Object item) {
        try {
            OffsetDateTime.parse(item.toString())
        } catch (Exception ignored) {
            false
        }
    }

    @Override
    void describeTo(Description description) {
        description.appendText("It must be parseable by OffsetDateTime")
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText("It must be parseable by OffsetDateTime")
    }
}
