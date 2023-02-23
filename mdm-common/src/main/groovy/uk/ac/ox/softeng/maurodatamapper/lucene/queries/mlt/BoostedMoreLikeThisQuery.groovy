/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.lucene.queries.mlt

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.IndexReader
import org.apache.lucene.queries.mlt.MoreLikeThis
import org.apache.lucene.queries.mlt.MoreLikeThisQuery
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query

/**
 * minTermFreq // filter out words that don't occur enough times in the source. defaults to 2
 * minDocFreq // filter out words that don't occur in enough docs. defaults to 5
 * maxDocFreq // filter out words that occur in too many docs. default to Integer.MAX_VALUE
 * minWordLength // filter out words that are noise by being less than the set length. defaults to 0 [ignore]
 *
 *
 * @since 23/10/2021
 */
class BoostedMoreLikeThisQuery extends MoreLikeThisQuery {

    Float boost
    String localFieldName
    int minWordLength = 0

    static final Set<String> STOP_WORDS = ['a', 'an', 'and', 'are', 'as', 'at', 'be', 'but', 'by',
                                           'for', 'if', 'in', 'into', 'is', 'it',
                                           'no', 'not', 'of', 'on', 'or', 'such',
                                           'that', 'the', 'their', 'then', 'there', 'these',
                                           'they', 'this', 'to', 'was', 'will', 'with'] as HashSet

    BoostedMoreLikeThisQuery(Analyzer analyzer, String fieldName, String likeText) {
        this(analyzer, fieldName, likeText, fieldName)
    }

    BoostedMoreLikeThisQuery(Analyzer analyzer, String fieldName, String likeText, String... moreLikeFields) {
        super(likeText, moreLikeFields, analyzer, fieldName)
        this.localFieldName = fieldName
        this.stopWords = STOP_WORDS
    }

    BoostedMoreLikeThisQuery boostedTo(float boost) {
        this.boost = boost
        this
    }

    BoostedMoreLikeThisQuery withMinWordLength(int minWordLength) {
        this.minWordLength = minWordLength
        this
    }

    BoostedMoreLikeThisQuery withMinDocFrequency(int minDocFrequency) {
        this.minDocFreq = minDocFrequency
        this
    }

    @Override
    Query rewrite(IndexReader reader) throws IOException {
        MoreLikeThis mlt = new MoreLikeThis(reader)

        mlt.setFieldNames(moreLikeFields)
        mlt.setAnalyzer(analyzer)
        mlt.setMinTermFreq(minTermFrequency)
        if (minDocFreq >= 0) {
            mlt.setMinDocFreq(minDocFreq)
        }
        mlt.setMaxQueryTerms(maxQueryTerms)
        mlt.setStopWords(stopWords)
        if (boost != null) {
            mlt.setBoost(true)
            mlt.setBoostFactor(boost)
        }
        mlt.setMinWordLen(minWordLength)

        BooleanQuery bq = (BooleanQuery) mlt.like(localFieldName, new StringReader(likeText))
        BooleanQuery.Builder newBq = new BooleanQuery.Builder()
        for (BooleanClause clause : bq) {
            newBq.add(clause)
        }
        //make at least half the terms match
        newBq.setMinimumNumberShouldMatch((int) (bq.clauses().size() * percentTermsToMatch))
        newBq.build()
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        BoostedMoreLikeThisQuery that = (BoostedMoreLikeThisQuery) o

        if (minWordLength != that.minWordLength) return false
        if (boost != that.boost) return false
        localFieldName == that.localFieldName
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (boost != null ? boost.hashCode() : 0)
        result = 31 * result + (localFieldName != null ? localFieldName.hashCode() : 0)
        result = 31 * result + minWordLength
        result
    }
}
