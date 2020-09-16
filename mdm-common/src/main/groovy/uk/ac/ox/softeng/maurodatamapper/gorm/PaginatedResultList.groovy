package uk.ac.ox.softeng.maurodatamapper.gorm

import grails.gorm.PagedResultList

class PaginatedResultList<E> extends PagedResultList<E> {

    Map<String, Object> pagination

    PaginatedResultList(List<E> results, Map<String, Object> pagination) {
        super(null)
        totalCount = results.size()
        this.pagination = pagination

        Integer max = pagination.max?.toInteger ?: 10
        Integer offset = pagination.offset?.toInteger ?: 0
        resultList = results.subList(Math.min(totalCount, offset), Math.min(totalCount, offset + max))
    }

    @Override
    protected void initialize() {
        // no-op, already initialized
    }
}
