package uk.ac.ox.softeng.maurodatamapper.core.search

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifierFilterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifiersFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.SearchParamFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.compiler.GrailsCompileStatic
import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.transform.CompileDynamic
import groovy.util.logging.Slf4j

@Slf4j
@GrailsCompileStatic
abstract class AbstractCatalogueItemSearchService {

    abstract List<Class<ModelItem>> getDomainsToSearch(SearchParams searchParams)

    List<Class<SearchParamFilter>> getSearchParamFilters() {
        [
            UpdatedBeforeFilter,
            UpdatedAfterFilter,
            CreatedBeforeFilter,
            CreatedAfterFilter,
            ClassifiersFilter,
            ClassifierFilterFilter
        ] as List<Class<SearchParamFilter>>
    }

    PaginatedLuceneResult<ModelItem> findAllModelItemsByOwningIdsByLuceneSearch(List<UUID> owningIds, SearchParams searchParams,
                                                                                Map pagination = [:]) {

        Closure additional = null

        List<Class<SearchParamFilter>> searchParamFilters = getSearchParamFilters()

        searchParamFilters.each {f ->
            SearchParamFilter filter = f.getDeclaredConstructor().newInstance()
            if (filter.doesApply(searchParams)) {
                if (additional) {
                    additional <<= filter.getClosure(searchParams)
                } else {
                    additional = filter.getClosure(searchParams)
                }
            }
        }
        List<Class<ModelItem>> domainsToSearch = getDomainsToSearch(searchParams)

        if (!domainsToSearch) {
            throw new ApiBadRequestException('SSXX', 'Owning IDs search attempted with filtered domains provided but no domains match this search ' +
                                                     'service')
        }

        long start = System.currentTimeMillis()

        List<ModelItem> modelItems

        if (searchParams.labelOnly) {
            log.debug('Performing lucene label search')
            modelItems = performLabelSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        } else {
            log.debug('Performing lucene standard search')
            modelItems = performStandardSearch(domainsToSearch, owningIds, searchParams.searchTerm, additional)
        }

        PaginatedLuceneResult<ModelItem> results = PaginatedLuceneResult.paginateFullResultSet(modelItems, pagination)

        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}")
        results
    }

    @CompileDynamic
    protected List<ModelItem> performLabelSearch(List<Class<ModelItem>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                                 @DelegatesTo(HibernateSearchApi) Closure additional = null) {

        domainsToSearch.collect {domain ->
            domain.luceneLabelSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten().findAll {!(it.id in owningIds)}
    }

    @CompileDynamic
    protected List<ModelItem> performStandardSearch(List<Class<ModelItem>> domainsToSearch, List<UUID> owningIds, String searchTerm,
                                                    @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        domainsToSearch.collect {domain ->
            domain.luceneStandardSearch(domain, searchTerm, owningIds, [:], additional).results
        }.flatten().findAll {!(it.id in owningIds)}
    }
}
