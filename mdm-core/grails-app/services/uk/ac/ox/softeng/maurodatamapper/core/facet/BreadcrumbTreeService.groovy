package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory

@Slf4j
@Transactional
class BreadcrumbTreeService {

    SessionFactory sessionFactory

    def finalise(BreadcrumbTree breadcrumbTree) {
        log.debug('Finalising BreadcrumbTree')
        long start = System.currentTimeMillis()
        breadcrumbTree.finalised = true
        String treeStringBefore = breadcrumbTree.treeString
        breadcrumbTree.buildTree()
        String treeStringAfter = breadcrumbTree.treeString
        String treeStringLike = "${treeStringBefore}%"
        sessionFactory.currentSession.createSQLQuery('UPDATE core.breadcrumb_tree ' +
                                                     'SET tree_string = REPLACE(tree_string, :treeStringBefore, :treeStringAfter) ' +
                                                     'WHERE tree_string LIKE :treeStringLike')
            .setParameter('treeStringBefore', treeStringBefore)
            .setParameter('treeStringAfter', treeStringAfter)
            .setParameter('treeStringLike', treeStringLike)
            .executeUpdate()
        breadcrumbTree.save(false)
        log.debug('BT finalisation took {}', Utils.timeTaken(start))
    }
}
