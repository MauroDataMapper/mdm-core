package uk.ac.ox.softeng.maurodatamapper.test.json.matcher

import net.javacrumbs.jsonunit.core.ParametrizedMatcher
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @since 15/10/2018
 */
class ErrorMessageMatcher extends BaseMatcher<String> implements ParametrizedMatcher {

    String regex


    @Override
    boolean matches(Object item) {
        if (item instanceof String) {
            item.matches(regex)
        }
        false
    }

    @Override
    void describeTo(Description description) {
        description.appendText("It must be match the expected message")
    }

    @Override
    void describeMismatch(Object item, Description description) {
        description.appendText("It must be match the expected message")
    }

    @Override
    void setParameter(String parameter) {
        this.regex = parameter
    }
}
