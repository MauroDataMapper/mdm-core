/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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
import static net.javacrumbs.jsonunit.JsonAssert.withMatcher

/**
 * @since 28/11/2017
 */
@Slf4j
trait JsonComparer {

    Configuration buildComparisonConfiguration(boolean exactOrder, Option... addtlOptions) {

        Configuration configuration = withMatcher('offsetDateTime', new OffsetDateTimeMatcher())
            .withMatcher('id', new IdMatcher())
            .withMatcher('version', new VersionMatcher())
            .withMatcher('errorMessage', new ErrorMessageMatcher())
            .withMatcher('fileContents', new FileContentsMatcher())

        Set<Option> optionsWith = new HashSet<>()
        if (!exactOrder) optionsWith.add(Option.IGNORING_ARRAY_ORDER)
        optionsWith.addAll(addtlOptions)

        if (optionsWith) {
            Option[] options = new Option[optionsWith.size() - 1]
            for (int i = 1; i < optionsWith.size(); i++) {
                options[i - 1] = optionsWith[i]
            }
            return configuration.when(optionsWith.first(), options)
        }
        configuration
    }

    // Wrap the assertion error so we get readable output to log and gradle
    void verifyJson(expected, actual, Option... addtlOptions) {
        verifyJson(expected, actual, true, false, addtlOptions)
    }

    void verifyJson(expected, actual, boolean replaceContentWithMatchersFlag, boolean exactOrderMatch, Option... addtlOptions) {

        try {
            assertJsonEquals(expected, actual, buildComparisonConfiguration(exactOrderMatch, addtlOptions))
        } catch (AssertionError error) {
            String body
            String toBeCleaned
            if (actual instanceof JSONObject) {
                JSONObject json = actual as JSONObject
                toBeCleaned = json.toString(2)
            } else {
                toBeCleaned = actual.toString()
            }

            String contentReplaced = replaceContentWithMatchersFlag ? replaceContentWithMatchers(toBeCleaned) : toBeCleaned
            body = prettyPrintJson(contentReplaced)

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

    String prettyPrintJson(String text) {
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