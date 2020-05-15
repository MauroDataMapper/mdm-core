package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.*
import uk.ac.ox.softeng.maurodatamapper.core.model.container.CatalogueItemClassifierAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.AnnotationAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MetadataAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.ReferenceFileAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.SemanticLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.search.Lucene
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import groovy.util.logging.Slf4j

/**
 * @since 06/12/2019
 */
@Slf4j
trait CatalogueItem<D extends Diffable> implements InformationAware, EditHistoryAware, Diffable<D>,
    CatalogueItemClassifierAware,
    MetadataAware,
    AnnotationAware,
    SemanticLinkAware,
    ReferenceFileAware {

    String aliasesString
    BreadcrumbTree breadcrumbTree

    void setAliases(Collection aliases) {
        String aliasString = ''
        if (aliases) {
            Collection<String> list
            if (aliases.first() instanceof Map) {
                list = aliases.collect {(it as Map).alias as String}
            } else list = aliases

            aliasString = list.collect {(it as String).trim()}?.join('|')
        }
        aliasesString = aliasString ?: null
    }

    Set<String> getAliases() {
        (aliasesString?.split(/\|/)?.findAll() ?: []) as Set
    }

    void beforeValidateCatalogueItem() {
        classifiers?.each {it.beforeValidate()}
        metadata?.each {it.beforeValidate()}
        annotations?.each {it.beforeValidate()}
        referenceFiles?.each {it.beforeValidate()}
    }

    @Override
    String getDiffIdentifier() {
        label
    }

    static <T extends CatalogueItem> ObjectDiff catalogueItemDiffBuilder(Class<T> diffClass, T lhs, T rhs) {
        String lhsId = lhs.id ?: "Left:Unsaved_${lhs.domainType}"
        String rhsId = rhs.id ?: "Right:Unsaved_${rhs.domainType}"
        ObjectDiff
            .builder(diffClass)
            .leftHandSide(lhsId, lhs)
            .rightHandSide(rhsId, rhs)
            .appendString('label', lhs.label, rhs.label)
            .appendString('description', lhs.description, rhs.description)
            .appendList(Metadata, 'metadata', lhs.metadata, rhs.metadata)
            .appendList(Annotation, 'annotations', lhs.annotations, rhs.annotations)
    }

    static <T extends CatalogueItem> DetachedCriteria<T> withCatalogueItemFilter(DetachedCriteria<T> criteria, Map filters) {
        if (filters.label) criteria = criteria.ilike('label', "%${filters.label}%")
        if (filters.description) criteria = criteria.ilike('description', "%${filters.description}%")
        if (filters.domainType) criteria = criteria.ilike('domainType', "%${filters.domainType}%")
        criteria
    }

    static <T extends CatalogueItem> PaginatedLuceneResult<T> luceneStandardSearch(Class<T> clazz, String searchTerm, List<UUID> allowedIds, Map
        pagination, @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        Lucene.securedPaginatedList(clazz, allowedIds, pagination) {
            if (searchTerm) {
                simpleQueryString(searchTerm, 'label', 'description', 'aliasesString', 'metadata.key', 'metadata.value')
            }
            if (additional) {
                additional.setResolveStrategy(Closure.DELEGATE_FIRST)
                additional.setDelegate(delegate)
                additional.call()
            }
        }
    }

    static <T extends CatalogueItem> PaginatedLuceneResult<T> luceneLabelSearch(Class<T> clazz, String searchTerm,
                                                                                List<UUID> allowedIds,
                                                                                @DelegatesTo(HibernateSearchApi) Closure additional) {
        luceneLabelSearch(clazz, searchTerm, allowedIds, [:], additional)
    }

    static <T extends CatalogueItem> PaginatedLuceneResult<T> luceneLabelSearch(Class<T> clazz, String searchTerm, List<UUID> allowedIds,
                                                                                Map pagination = [:],
                                                                                @DelegatesTo(HibernateSearchApi) Closure additional = null) {
        Lucene.securedPaginatedList(clazz, allowedIds, pagination, additional) {
            simpleQueryString searchTerm, 'label'
        }
    }
}