package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.container


import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus

import java.nio.charset.Charset

import static uk.ac.ox.softeng.maurodatamapper.test.http.RestClientInterface.MAP_ARG

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

class TestMergeVersionedFolderBuilder {

    BaseFunctionalSpec functionalSpec

    TestMergeVersionedFolderBuilder(BaseFunctionalSpec functionalSpec) {
        this.functionalSpec = functionalSpec
    }

    def buildCommonAncestorDataModel(String commonAncestorId) {
        POST("folders/$commonAncestorId/dataModels", [
            label: 'Functional Test DataModel 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String dataModel1Id = responseBody().id

        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteRightOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyRightOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndDelete'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'deleteAndModify'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndDelete'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningNoDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'modifyAndModifyReturningDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses", [label: 'existingClass'], MAP_ARG, true)
        verifyResponse CREATED, response
        String caExistingClass = responseBody().id
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteLeftOnlyFromExistingClass'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/dataClasses/$caExistingClass/dataClasses", [label: 'deleteRightOnlyFromExistingClass'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'], MAP_ARG,
             true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'],
             MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$dataModel1Id/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'],
             MAP_ARG, true)
        verifyResponse CREATED, response
    }

    Map modifySourceDataModel(String source) {
        // Modify Source
        Map sourceMap = [
            dataModelId                         : getIdFromPath(source, 'dm:Functional Test DataModel 1$source'),
            existingClass                       : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:existingClass'),
            deleteLeftOnlyFromExistingClass     :
                getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass'),
            deleteLeftOnly                      : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:deleteLeftOnly'),
            modifyLeftOnly                      : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:modifyLeftOnly'),
            deleteAndDelete                     : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:deleteAndDelete'),
            deleteAndModify                     : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:deleteAndModify'),
            modifyAndDelete                     : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:modifyAndDelete'),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:modifyAndModifyReturningNoDifference'),
            modifyAndModifyReturningDifference  :
                getIdFromPath(source, 'dm:Functional Test DataModel 1$source|dc:modifyAndModifyReturningDifference'),
            dataModelMetadataModifyOnSource     : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|md:functional.test.modifyOnSource'),
            dataModelMetadataDeleteFromSource   : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|md:functional.test.deleteFromSource'),
            dataModelMetadataModifyAndDelete    : getIdFromPath(source, 'dm:Functional Test DataModel 1$source|md:functional.test.modifyAndDelete'),
        ]

        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses/$sourceMap.deleteLeftOnlyFromExistingClass",
               MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteLeftOnly", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.deleteAndModify", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyLeftOnly", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndDelete", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningNoDifference", [description: 'Description'], MAP_ARG,
            true)
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'], MAP_ARG,
            true)
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/dataClasses/$sourceMap.existingClass/dataClasses", [label: 'addLeftToExistingClass'], MAP_ARG, true)
        verifyResponse CREATED, response
        sourceMap.addLeftToExistingClass = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$sourceMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionLeft'], MAP_ARG, true)
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        PUT("dataModels/$sourceMap.dataModelId", [description: 'DescriptionLeft'], MAP_ARG, true)
        verifyResponse OK, response

        POST("dataModels/$sourceMap.dataModelId/metadata", [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'],
             MAP_ARG, true)
        verifyResponse CREATED, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.dataModelMetadataModifyOnSource", [value: 'source has modified this'], MAP_ARG,
            true)
        verifyResponse OK, response
        PUT("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.dataModelMetadataModifyAndDelete", [value: 'source has modified this also'],
            MAP_ARG, true)
        verifyResponse OK, response
        DELETE("dataModels/$sourceMap.dataModelId/metadata/$sourceMap.dataModelMetadataDeleteFromSource", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetDataModel(String target) {
        // Modify Target
        Map targetMap = [
            dataModelId                         : getIdFromPath(target, 'dm:Functional Test DataModel 1$main'),
            existingClass                       : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:existingClass'),
            deleteRightOnly                     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:deleteRightOnly'),
            modifyRightOnly                     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:modifyRightOnly'),
            deleteRightOnlyFromExistingClass    :
                getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:existingClass|dc:deleteRightOnlyFromExistingClass'),
            deleteAndDelete                     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:deleteAndDelete'),
            deleteAndModify                     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:deleteAndModify'),
            modifyAndDelete                     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:modifyAndDelete'),
            modifyAndModifyReturningNoDifference:
                getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:modifyAndModifyReturningNoDifference'),
            modifyAndModifyReturningDifference  : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:modifyAndModifyReturningDifference'),
            deleteLeftOnly                      : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:deleteLeftOnly'),
            modifyLeftOnly                      : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:modifyLeftOnly'),
            deleteLeftOnlyFromExistingClass     : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|dc:deleteLeftOnlyFromExistingClass'),
            metadataModifyAndDelete             : getIdFromPath(target, 'dm:Functional Test DataModel 1$main|md:functional.test.modifyAndDelete'),
        ]

        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses/$targetMap.deleteRightOnlyFromExistingClass",
               MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteRightOnly", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyRightOnly", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.deleteAndModify", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningNoDifference", [description: 'Description'], MAP_ARG,
            true)
        verifyResponse OK, response
        PUT("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'], MAP_ARG,
            true)
        verifyResponse OK, response

        POST("dataModels/$targetMap.dataModelId/dataClasses/$targetMap.existingClass/dataClasses", [label: 'addRightToExistingClass'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addRightOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningNoDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("dataModels/$targetMap.dataModelId/dataClasses", [label: 'addAndAddReturningDifference', description: 'DescriptionRight'], MAP_ARG, true)
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("dataModels/$targetMap.dataModelId", [description: 'DescriptionRight'], MAP_ARG, true)
        verifyResponse OK, response
        DELETE("dataModels/$targetMap.dataModelId/metadata/$targetMap.metadataModifyAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        targetMap
    }

    def buildCommonAncestorTerminology(String commonAncestorId) {
        POST("folders/${commonAncestorId}/terminologies", [
            label: 'Functional Test Terminology 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String baseTerminology = responseBody().id

        POST("terminologies/$baseTerminology/terms", [code: 'DLO', definition: 'deleteLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'MLO', definition: 'modifyLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'DAM', definition: 'deleteAndModify'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'MAD', definition: 'modifyAndDelete'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'MAMRD', definition: 'modifyAndModifyReturningDifference'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'],
             MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'], MAP_ARG,
             true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'],
             MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'],
             MAP_ARG, true)
        verifyResponse CREATED, response
    }

    Map modifySourceTerminology(String source) {
        // Modify Source
        Map sourceMap = [
            terminologyId                     : getIdFromPath(source, 'te:Functional Test Terminology 1$source'),
            deleteLeftOnly                    : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:DLO'),
            modifyLeftOnly                    : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MLO'),
            deleteAndModify                   : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:DAM'),
            modifyAndDelete                   : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MAD'),
            modifyAndModifyReturningDifference: getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MAMRD'),
            metadataModifyOnSource            : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.modifyAndDelete'),
        ]

        DELETE("terminologies/$sourceMap.terminologyId/terms/$sourceMap.deleteLeftOnly", MAP_ARG, true)
        verifyResponse NO_CONTENT, response
        DELETE("terminologies/$sourceMap.terminologyId/terms/$sourceMap.deleteAndModify", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyLeftOnly", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyAndDelete", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'], MAP_ARG,
            true)
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/terms", [code: 'ALO', definition: 'addLeftOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("terminologies/$sourceMap.terminologyId/terms", [
            code       : 'AAARD',
            definition : 'addAndAddReturningDifference',
            description: 'DescriptionLeft'], MAP_ARG, true)
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        PUT("terminologies/$sourceMap.terminologyId", [description: 'DescriptionLeft'], MAP_ARG, true)
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/metadata",
             [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'], MAP_ARG, true)
        verifyResponse CREATED, response
        PUT("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'],
            MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'],
            MAP_ARG, true)
        verifyResponse OK, response
        DELETE("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataDeleteFromSource", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetTerminology(String target) {
        // Modify Target
        Map targetMap = [
            terminologyId                     : getIdFromPath(target, 'te:Functional Test Terminology 1$main'),
            deleteLeftOnly                    : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:DLO'),
            modifyLeftOnly                    : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MLO'),
            deleteAndModify                   : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:DAM'),
            modifyAndDelete                   : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MAD'),
            modifyAndModifyReturningDifference: getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MAMRD'),
            metadataModifyOnSource            : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.modifyAndDelete'),
        ]

        DELETE("terminologies/$targetMap.terminologyId/terms/$targetMap.modifyAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$targetMap.terminologyId/terms/$targetMap.deleteAndModify", [description: 'Description'], MAP_ARG, true)
        verifyResponse OK, response
        PUT("terminologies/$targetMap.terminologyId/terms/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'], MAP_ARG,
            true)
        verifyResponse OK, response

        POST("terminologies/$targetMap.terminologyId/terms", [code: 'ARO', definition: 'addRightOnly'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("terminologies/$targetMap.terminologyId/terms",
             [code: 'AAARD', definition: 'addAndAddReturningDifference', description: 'DescriptionRight'], MAP_ARG, true)
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("terminologies/$targetMap.terminologyId", [description: 'DescriptionRight'], MAP_ARG, true)
        verifyResponse OK, response
        DELETE("terminologies/$targetMap.terminologyId/metadata/$targetMap.metadataModifyAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        targetMap
    }

    def buildCommonAncestorCodeSet(String commonAncestorId) {
        POST("folders/$commonAncestorId/codeSets", [
            label: 'Functional Test CodeSet 1'
        ], MAP_ARG, true)
        verifyResponse(CREATED, response)
        String codeSetId = responseBody().id
        Map caIds = [
            terminologyId                     : getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main'),
            deleteLeftOnly                    : getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main|tm:DLO'),
            modifyLeftOnly                    : getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main|tm:MLO'),
            deleteAndModify                   : getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main|tm:DAM'),
            modifyAndDelete                   : getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main|tm:MAD'),
            modifyAndModifyReturningDifference: getIdFromPath(commonAncestorId, 'te:Functional Test Terminology 1$main|tm:MAMRD'),
        ]
        PUT("codeSets/$codeSetId", [terms: [
            [id: caIds.deleteLeftOnly],
            [id: caIds.modifyLeftOnly],
            [id: caIds.deleteAndModify],
            [id: caIds.modifyAndDelete],
            [id: caIds.modifyAndModifyReturningDifference]
        ]], MAP_ARG, true)
        verifyResponse OK, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'], MAP_ARG,
             true)
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'], MAP_ARG, true)
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'],
             MAP_ARG, true)
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'],
             MAP_ARG, true)
        verifyResponse CREATED, response
    }

    Map modifySourceCodeSet(String source) {
        // Modify Source
        Map sourceMap = [
            codeSetId                         : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source'),
            modifyLeftOnly                    : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|tm:MLO'),
            modifyAndDelete                   : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|tm:MAD'),
            modifyAndModifyReturningDifference: getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|tm:MAMRD'),
            addLeftOnly                       : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:ALO'),
            addAndAddReturningDifference      : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:AAARD'),
            metadataModifyOnSource            : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(source, 'cs:Functional Test CodeSet 1$source|md:functional.test.modifyAndDelete'),
        ]

        PUT("codeSets/$sourceMap.codeSetId", [description: 'DescriptionLeft'], MAP_ARG, true)
        verifyResponse OK, response

        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addLeftOnly", [:], MAP_ARG, true)
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addAndAddReturningDifference", [:], MAP_ARG, true)
        verifyResponse OK, response

        POST("codeSets/$sourceMap.codeSetId/metadata",
             [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'], MAP_ARG, true)
        verifyResponse CREATED, response
        PUT("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'],
            MAP_ARG, true)
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'],
            MAP_ARG, true)
        verifyResponse OK, response
        DELETE("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataDeleteFromSource", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetCodeSet(String target) {
        // Modify Target
        Map targetMap = [
            codeSetId                         : getIdFromPath(target, 'cs:Functional Test CodeSet 1$main'),
            modifyLeftOnly                    : getIdFromPath(target, 'cs:Functional Test CodeSet 1$main|tm:MLO'),
            modifyAndModifyReturningDifference: getIdFromPath(target, 'cs:Functional Test CodeSet 1$main|tm:MAMRD'),
            addAndAddReturningDifference      : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:AAARD'),
            metadataModifyOnSource            : getIdFromPath(target, 'cs:Functional Test CodeSet 1$main|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(target, 'cs:Functional Test CodeSet 1$main|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(target, 'cs:Functional Test CodeSet 1$main|md:functional.test.modifyAndDelete'),
        ]
        PUT("codeSets/$targetMap.codeSetId/terms/$targetMap.addAndAddReturningDifference", [:], MAP_ARG, true)
        verifyResponse OK, response

        DELETE("codeSets/$targetMap.codeSetId/metadata/$targetMap.metadataModifyAndDelete", MAP_ARG, true)
        verifyResponse NO_CONTENT, response

        targetMap
    }


    def <O> HttpResponse<O> POST(String resourceEndpoint, Map body, Argument<O> bodyType = MAP_ARG,
                                 boolean cleanEndpoint = false) {
        functionalSpec.POST(resourceEndpoint, body, bodyType, cleanEndpoint)
    }

    def <O> HttpResponse<O> PUT(String resourceEndpoint, Map body, Argument<O> bodyType = MAP_ARG,
                                boolean cleanEndpoint = false) {
        functionalSpec.PUT(resourceEndpoint, body, bodyType, cleanEndpoint)
    }

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Argument<O> bodyType = MAP_ARG,
                                   boolean cleanEndpoint = false) {
        functionalSpec.DELETE(resourceEndpoint, bodyType, cleanEndpoint)
    }

    def <O> HttpResponse<O> GET(String resourceEndpoint, Argument<O> bodyType = MAP_ARG, boolean cleanEndpoint = false) {
        functionalSpec.GET(resourceEndpoint, bodyType, cleanEndpoint)
    }

    void verifyResponse(HttpStatus expectedStatus, HttpResponse<Map> response) {
        functionalSpec.verifyResponse(expectedStatus, response)
    }

    HttpResponse<Map> getResponse() {
        functionalSpec.response
    }

    Map<String, Object> responseBody() {
        functionalSpec.responseBody()
    }

    String getIdFromPath(String rootResourceId, String path) {
        GET("$rootResourceId/path/${URLEncoder.encode(path, Charset.defaultCharset())}")
        verifyResponse OK, response
        assert responseBody().id
        responseBody().id
    }

    static String getExpectedMergeDiffJson() {
        '''{
  "sourceId": "${json-unit.matches:id}",
  "targetId": "${json-unit.matches:id}",
  "path": "vf:Functional Test VersionedFolder 3$source",
  "label": "Functional Test VersionedFolder 3",
  "count": 34,
  "diffs": [
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source@description",
      "sourceValue": "source description on the versioned folder",
      "targetValue": "Target modified description",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|tm:ALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|cs:Functional Test CodeSet 1$source|tm:AAARD@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:addLeftOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:deleteAndModify",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:deleteLeftOnly",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:addAndAddReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:existingClass|dc:addLeftToExistingClass",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:existingClass|dc:deleteLeftOnlyFromExistingClass",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:modifyAndModifyReturningDifference@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|dc:modifyLeftOnly@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder 3$source|dm:Functional Test DataModel 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|md:functional.test.addToSourceOnly",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|md:functional.test.modifyAndDelete",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|md:functional.test.deleteFromSource",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "value",
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|md:functional.test.modifyOnSource@value",
      "sourceValue": "source has modified this",
      "targetValue": "some original value",
      "commonAncestorValue": "some original value",
      "isMergeConflict": false,
      "type": "modification"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:ALO",
      "isMergeConflict": false,
      "isSourceModificationAndTargetDeletion": false,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:MAD",
      "isMergeConflict": true,
      "isSourceModificationAndTargetDeletion": true,
      "type": "creation"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:DAM",
      "isMergeConflict": true,
      "isSourceDeletionAndTargetModification": true,
      "type": "deletion"
    },
    {
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:DLO",
      "isMergeConflict": false,
      "isSourceDeletionAndTargetModification": false,
      "type": "deletion"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:AAARD@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:MAMRD@description",
      "sourceValue": "DescriptionLeft",
      "targetValue": "DescriptionRight",
      "commonAncestorValue": null,
      "isMergeConflict": true,
      "type": "modification"
    },
    {
      "fieldName": "description",
      "path": "vf:Functional Test VersionedFolder 3$source|te:Functional Test Terminology 1$source|tm:MLO@description",
      "sourceValue": "Description",
      "targetValue": null,
      "commonAncestorValue": null,
      "isMergeConflict": false,
      "type": "modification"
    }
  ]
}'''
    }
}
