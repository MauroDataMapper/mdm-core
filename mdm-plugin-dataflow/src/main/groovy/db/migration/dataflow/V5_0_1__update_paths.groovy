package db.migration.dataflow

import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        DataFlow.withNewTransaction {
            List<DataFlow> dataFlows = DataFlow.list()
            dataFlows.each { df ->
                df.checkPath()
                df.dataClassComponents.each { dc -> dc.checkPath() }
                df.validate()
                df.save(flush:true, validate:false)
            }
        }
    }
}
