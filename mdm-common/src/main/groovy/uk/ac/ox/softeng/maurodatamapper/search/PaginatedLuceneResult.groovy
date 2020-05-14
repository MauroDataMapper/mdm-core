package uk.ac.ox.softeng.maurodatamapper.search


import groovy.util.logging.Slf4j

/**
 * @since 27/04/2018
 */
@Slf4j
class PaginatedLuceneResult<K> {

    int count
    List<K> results

    PaginatedLuceneResult(List<K> results, int count) {
        this.count = count
        this.results = results
    }

    static <D> PaginatedLuceneResult<D> paginateFullResultSet(List<D> fullResultSet, Map pagination) {

        if (!pagination) return new PaginatedLuceneResult<D>(fullResultSet, fullResultSet.size())

        Integer max = pagination.max?.toInteger()
        Integer offsetAmount = pagination.offset?.toInteger()
        String sortKey = pagination.sort ?: 'label'
        String order = pagination.order ?: 'asc'


        List<D> sortedList = fullResultSet.sort {a, b ->
            if (order == 'asc') {
                a."$sortKey" <=> b."${sortKey}"
            } else {
                b."$sortKey" <=> a."${sortKey}"
            }
        }

        List<D> offsetList = offsetAmount == null ? sortedList : sortedList.drop(offsetAmount)
        List<D> maxList = max == null ? offsetList : offsetList.take(max)
        new PaginatedLuceneResult<D>(maxList, maxList.size())
    }
}
