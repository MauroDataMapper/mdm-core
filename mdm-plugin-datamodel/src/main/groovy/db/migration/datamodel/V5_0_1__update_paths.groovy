package db.migration.datamodel


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        DataModel.withNewTransaction {
            List<DataModel> dataModels = DataModel.list()
            dataModels.each {dm ->
                dm.checkPath()
                dm.breadcrumbTree.update(dm)
                dm.skipValidation(true)
                log.debug('Path Migrating {}', dm.path)
                dm.fullSortOfChildren(dm.dataTypes)
                dm.fullSortOfChildren(dm.childDataClasses)
                dm.childDataClasses.each {dc -> checkDataClass(dc)}
                dm.dataTypes.each {
                    it.checkPath()
                    it.breadcrumbTree.update(it)
                    it.skipValidation(true)
                    if (it instanceof EnumerationType) {
                        it.fullSortOfChildren(it.enumerationValues)
                        it.enumerationValues.each {ev ->
                            ev.checkPath()
                            ev.breadcrumbTree.update(ev)
                            ev.skipValidation(true)
                        }
                    }
                }
                dm.save(flush: true, validate: false)

            }
        }
    }

    void checkDataClass(DataClass dataClass) {
        dataClass.checkPath()
        dataClass.skipValidation(true)
        dataClass.breadcrumbTree.update(dataClass)
        if (dataClass.dataElements) {
            dataClass.dataElements.each {
                it.checkPath()
                it.breadcrumbTree.update(it)
                it.skipValidation(true)
            }
            dataClass.fullSortOfChildren(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            dataClass.fullSortOfChildren(dataClass.dataClasses)
            dataClass.dataClasses.each {
                checkDataClass(it)
                it.breadcrumbTree.update(it)
                it.skipValidation(true)
            }
        }
    }
}
