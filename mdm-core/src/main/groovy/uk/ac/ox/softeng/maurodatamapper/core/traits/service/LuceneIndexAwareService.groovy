package uk.ac.ox.softeng.maurodatamapper.core.traits.service

/**
 * @since 18/10/2019
 */
interface LuceneIndexAwareService {

    void beforeRebuild(session)

    void afterRebuild()
}