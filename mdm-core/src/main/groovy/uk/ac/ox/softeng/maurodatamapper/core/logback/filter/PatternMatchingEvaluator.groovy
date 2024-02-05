/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.logback.filter

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.boolex.EvaluationException
import ch.qos.logback.core.boolex.EventEvaluatorBase

import java.util.regex.Pattern

/**
 * @since 01/02/2022
 */
class PatternMatchingEvaluator extends EventEvaluatorBase<ILoggingEvent> {

    private List<Pattern> patterns

    PatternMatchingEvaluator(List<Pattern> patterns) {
        name = 'Pattern Matching Evaluator'
        this.patterns = patterns
        start()
    }

    @Override
    boolean evaluate(ILoggingEvent event) throws NullPointerException, EvaluationException {
        patterns.any {event.message.matches(it)}
    }
}
