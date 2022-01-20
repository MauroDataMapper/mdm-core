package db.migration.referencedata

import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        ReferenceDataModel.withNewTransaction {
            List<ReferenceDataModel> referenceDataModels = ReferenceDataModel.list()
            referenceDataModels.each { dm ->
                dm.checkPath()
                log.debug('Path Migrating {}', dm.path)
                dm.referenceDataElements.each { it.checkPath() }
                dm.referenceDataTypes.each{it.checkPath()}
                dm.referenceDataValues.each{it.checkPath()}
                dm.validate()
                dm.save(flush:true, validate:false)
            }
        }
    }
}
