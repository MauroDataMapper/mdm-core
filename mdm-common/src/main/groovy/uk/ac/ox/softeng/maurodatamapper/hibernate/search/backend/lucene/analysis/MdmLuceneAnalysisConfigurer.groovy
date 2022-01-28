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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.backend.lucene.analysis

import org.apache.lucene.analysis.core.LowerCaseFilterFactory
import org.apache.lucene.analysis.core.StopFilterFactory
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory
import org.apache.lucene.analysis.pattern.PatternTokenizerFactory
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer

/**
 * @since 12/10/2021
 */
class MdmLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

    @Override
    void configure(LuceneAnalysisConfigurationContext context) {

        context.analyzer('wordDelimiter').custom()
            .tokenizer(WhitespaceTokenizerFactory)
            .tokenFilter(WordDelimiterGraphFilterFactory)
            .tokenFilter(LowerCaseFilterFactory)

        context.analyzer('lowercase').custom()
            .tokenizer(WhitespaceTokenizerFactory)
            .tokenFilter(ASCIIFoldingFilterFactory)
            .tokenFilter(LowerCaseFilterFactory)

        context.analyzer('path').custom()
            .tokenizer(PatternTokenizerFactory)
            .param(PatternTokenizerFactory.PATTERN, '/')
            .param(PatternTokenizerFactory.GROUP, '-1')
            .tokenFilter(LowerCaseFilterFactory)

        context.analyzer('pipe').custom()
            .tokenizer(PatternTokenizerFactory)
            .param(PatternTokenizerFactory.PATTERN, '\\|')
            .param(PatternTokenizerFactory.GROUP, '-1')
            .tokenFilter(LowerCaseFilterFactory)
            .tokenFilter(StopFilterFactory) // Defaults to org.apache.lucene.analysis.en.EnglishAnalyzer.ENGLISH_STOP_WORDS_SET

        context.normalizer('lowercase').custom()
            .tokenFilter(ASCIIFoldingFilterFactory)
            .tokenFilter(LowerCaseFilterFactory)

    }

}


