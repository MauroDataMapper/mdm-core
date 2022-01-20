package db.migration.terminology


import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        Terminology.withNewTransaction {
            List<Terminology> terminologies = Terminology.list()
            terminologies.each {t ->
                t.checkPath()
                t.breadcrumbTree.update(t)
                t.skipValidation(true)
                log.debug('Path Migrating {}', t.path)

                t.terms.each {te ->
                    te.checkPath()
                    te.breadcrumbTree.update(te)
                    te.skipValidation(true)
                    te.sourceTermRelationships.each {tr ->
                        tr.checkPath()
                        tr.breadcrumbTree.update(tr)
                        tr.skipValidation(true)
                    }
                }
                t.termRelationshipTypes.each {
                    it.checkPath()
                    it.breadcrumbTree.update(it)
                    it.skipValidation(true)
                }
                t.save(flush: true, validate: false)
            }
        }
    }
}
