package uk.ac.ox.softeng.maurodatamapper.test.json

import uk.ac.ox.softeng.maurodatamapper.test.json.matcher.ErrorMessageMatcher
import uk.ac.ox.softeng.maurodatamapper.test.json.matcher.FileContentsMatcher
import uk.ac.ox.softeng.maurodatamapper.test.json.matcher.IdMatcher
import uk.ac.ox.softeng.maurodatamapper.test.json.matcher.OffsetDateTimeMatcher
import uk.ac.ox.softeng.maurodatamapper.test.json.matcher.VersionMatcher

import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import net.javacrumbs.jsonunit.core.Configuration
import net.javacrumbs.jsonunit.core.Option
import org.grails.web.json.JSONObject
import org.junit.Assert

import java.util.regex.Pattern

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals
import static net.javacrumbs.jsonunit.JsonAssert.when

/**
 * @since 28/11/2017
 */
@Slf4j
trait JsonComparer {

    Collection<Option> getDefaultCompareOptions() {
        [Option.IGNORING_ARRAY_ORDER]
    }

    Configuration buildComparisonConfiguration(Option... addtlOptions) {
        EnumSet<Option> optionsWith = EnumSet.copyOf(defaultCompareOptions)
        optionsWith.addAll(addtlOptions)
        Option[] options = new Option[optionsWith.size() - 1]
        for (int i = 1; i < optionsWith.size(); i++) {
            options[i - 1] = optionsWith[i]
        }
        when(optionsWith.first(), options)
            .withMatcher('offsetDateTime', new OffsetDateTimeMatcher())
            .withMatcher('id', new IdMatcher())
            .withMatcher('version', new VersionMatcher())
            .withMatcher('errorMessage', new ErrorMessageMatcher())
            .withMatcher('fileContents', new FileContentsMatcher())
    }

    // Wrap the assertion error so we get readable output to log and gradle
    void verifyJson(expected, actual, Option... addtlOptions) {

        try {
            assertJsonEquals(expected, actual, buildComparisonConfiguration(addtlOptions))
        } catch (AssertionError error) {
            String body
            String toBeCleaned
            if (actual instanceof JSONObject) {
                JSONObject json = actual as JSONObject
                toBeCleaned = json.toString(2)
            } else {
                toBeCleaned = actual.toString()
            }

            String cleaned = replaceContentWithMatchers(toBeCleaned)
            String prettyPrinted = prettyPrint(cleaned)
            body = replaceContentWithMatchers(prettyPrinted)

            String message = reformatJsonDiffErrorMessage(error.message)

            log.error('JSON objects do not match\n\n{}', message)
            log.warn('Actual JSON\n{}\n', body)

            Assert.fail(message)
        } catch (IllegalArgumentException ex) {
            log.error('{}', expected)
            throw ex
        }
    }

    String replaceContentWithMatchers(String jsonString) {
        Pattern p = Pattern.compile(/"fileContents": \[.+?]/, Pattern.DOTALL)
        jsonString
            .replaceAll(/"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[34][0-9a-fA-F]{3}-[89ab][0-9a-fA-F]{3}-[0-9a-fA-F]{12}"/,
                        '"\\\${json-unit.matches:id}"')
            .replaceAll(/"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,6})?Z"/,
                        '"\\\${json-unit.matches:offsetDateTime}"')
            .replaceAll(p, '"fileContents": "\\\${json-unit.matches:fileContents}"')
    }

    String prettyPrint(String text) {
        try {
            return JsonOutput.prettyPrint(text).replaceAll(/ {4}/, '  ')
        } catch (Exception ignored) {}
        text
    }

    String reformatJsonDiffErrorMessage(String message) {
        message
            .replaceAll(/\.\s*Expected\s*/, ':\n  - Expected ')

            .replaceAll(/,\s*expected:\s<*/, ':\n  - Expected: <')
            .replaceAll(/>\s*but was:\s*</, '>\n  - Got     : <')

            .replaceAll(/,\s*got\s*/, '\n  - Got ')
            .replaceAll(/\.\s*Extra:\s*"/, '\n  - Extra: "')
            .replaceAll(/\.\s*Missing:\s*/, '\n  - Missing: ')
            .replaceAll(/\.\s*Missing values\s*/, ':\n  - Missing values: ')
            .replaceAll(/,\s*extra values\s*/, '\n  - Extra values: "')
    }
}