/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.search.filter

import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.hibernate.search.annotations.Factory

/**
 * @since 27/04/2018
 */
class IdPathSecureFilterFactory {

    private Set<UUID> allowedIds

    void setAllowedIds(Collection<UUID> allowedIds) {
        this.allowedIds = allowedIds.toSet()
    }

    @Factory
    Query create() {

        BooleanQuery.Builder builder = new BooleanQuery.Builder()
        allowedIds.each {id ->
            builder.add(new TermQuery(new Term('id', id.toString())), BooleanClause.Occur.SHOULD)
            builder.add(new TermQuery(new Term('path', id.toString())), BooleanClause.Occur.SHOULD)
        }

        new BooleanQuery.Builder().add(builder.build(), BooleanClause.Occur.FILTER).build()
    }
}
