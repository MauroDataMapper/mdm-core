package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import uk.ac.ox.softeng.maurodatamapper.util.Version

import org.hamcrest.BaseMatcher
import org.hamcrest.Description


/**
 * @since 29/11/2017
 */
class VersionMatcher extends BaseMatcher<Version> {
    @Override
    boolean matches(Object item) {
        if (item instanceof String) {
            return Version.isVersionable(item.replaceAll(/(-SNAPSHOT|\.RC\d)/, ''))
        }
        false

    }

    @Override
    void describeTo(Description description) {
        description.appendText("It must be parseable to a version")
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText("It must be parseable to a version")
    }
}
