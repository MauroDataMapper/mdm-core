package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search

import grails.validation.Validateable

class SearchParams implements Validateable {

    String domainType
    Integer max
    Integer offset
    String searchTerm
    Boolean labelOnly = false
    List<String> domainTypes = []
    List<String> dataModelTypes = []
    Date lastUpdatedBefore
    Date lastUpdatedAfter
    Date createdBefore
    Date createdAfter
    List<String> classifiers = []
    List<List> classifierFilter = []
    String sortField = ""

    static constraints = {
        domainType nullable: true
        offset nullable: true, min: 0
        max nullable: true, min: 0
        searchTerm nullable: true, blank: false
        lastUpdatedBefore nullable: true
        lastUpdatedAfter nullable: true
        createdBefore nullable: true
        createdAfter nullable: true
    }

    void setLimit(Integer limit) {
        max = limit
    }

    void setSearch(String term) {
        searchTerm = term
    }

    List<String> getDomainTypes() {
        domainTypes ?: domainType ? [domainType] : []
    }
}
