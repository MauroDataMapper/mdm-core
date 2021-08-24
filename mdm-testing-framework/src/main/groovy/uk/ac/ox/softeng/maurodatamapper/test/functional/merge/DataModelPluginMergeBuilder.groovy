package uk.ac.ox.softeng.maurodatamapper.test.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 03/08/2021
 */
class DataModelPluginMergeBuilder extends BaseTestMergeBuilder {

    DataModelPluginMergeBuilder(BaseFunctionalSpec functionalSpec) {
        super(functionalSpec)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        String ca = buildCommonAncestorDataModel(folderId)

        PUT("dataModels/$ca/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("dataModels/$ca/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("dataModels/$ca/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        Map<String, Object> sourceMap = modifySourceDataModel(source)
        Map<String, Object> targetMap = modifyTargetDataModel(target)

        new TestMergeData(source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap
        )
    }

    String buildCommonAncestorDataModel(String folderId, String suffix = 1) {
        POST("folders/$folderId/dataModels", [
            label: "Functional Test DataModel ${suffix}".toString()
        ])
        verifyResponse(CREATED, response)
        String dataModel1Id = responseBody().id

        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndDelete'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndModify'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndDelete'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'existingClass'])
        verifyResponse CREATED, response
        String caExistingClass = responseBody().id
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteRightOnlyFromExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        verifyResponse CREATED, response

        dataModel1Id
    }

    Map modifySourceDataModel(String source, String suffix = '1', String pathing = '') {
        // Modify Source
        Map sourceMap = [
            dataModelId                         : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source"),
            existingClass                       : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:existingClass"),
            deleteLeftOnlyFromExistingClass     :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass"),
            deleteLeftOnly                      : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyLeftOnly"),
            deleteAndDelete                     : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteAndDelete"),
            deleteAndModify                     : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:deleteAndModify"),
            modifyAndDelete                     : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  :
                getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|dc:modifyAndModifyReturningDifference"),
            metadataModifyOnSource              : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource            : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete             : getIdFromPath(source, "${pathing}dm:Functional Test DataModel ${suffix}\$source|md:functional.test.modifyAndDelete"),
        ]

        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses/$sourceMap.deleteLeftOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses", [label: 'addLeftToExistingClass'])
        verifyResponse CREATED, response
        sourceMap.addLeftToExistingClass = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addLeftOnly'])
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'])
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        PUT("dataModels/$sourceMap.dataModelId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/metadata", [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        verifyResponse CREATED, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        verifyResponse OK, response
        DELETE("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.metadataDeleteFromSource")
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetDataModel(String target, String suffix = '1', String pathing = '') {
        // Modify Target
        Map targetMap = [
            dataModelId                         : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main"),
            existingClass                       : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:existingClass"),
            deleteRightOnly                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteRightOnly"),
            modifyRightOnly                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyRightOnly"),
            deleteRightOnlyFromExistingClass    :
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:existingClass|dc:deleteRightOnlyFromExistingClass"),
            deleteAndDelete                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteAndDelete"),
            deleteAndModify                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteAndModify"),
            modifyAndDelete                     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndDelete"),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndModifyReturningNoDifference"),
            modifyAndModifyReturningDifference  : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyAndModifyReturningDifference"),
            deleteLeftOnly                      : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteLeftOnly"),
            modifyLeftOnly                      : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:modifyLeftOnly"),
            deleteLeftOnlyFromExistingClass     : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|dc:deleteLeftOnlyFromExistingClass"),
            metadataModifyAndDelete             : getIdFromPath(target, "${pathing}dm:Functional Test DataModel ${suffix}\$main|md:functional.test.modifyAndDelete"),
        ]

        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses/$targetMap.deleteRightOnlyFromExistingClass")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteRightOnly")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndDelete")
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyRightOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningNoDifference", [description: 'Description'])
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses", [label: 'addRightToExistingClass'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addRightOnly'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'])
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("dataModels/$targetMap.dataModelId", [description: 'DescriptionRight'])
        verifyResponse OK, response
        DELETE("dataModels/$targetMap.dataModelId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }
}
