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
