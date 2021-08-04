package uk.ac.ox.softeng.maurodatamapper.test.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.BaseTestMergeBuilder

import java.nio.charset.Charset

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK

/**
 * @since 03/08/2021
 */
class TerminologyPluginMergeBuilder extends BaseTestMergeBuilder {

    TerminologyPluginMergeBuilder(BaseFunctionalSpec functionalSpec) {
        super(functionalSpec)
    }

    @Override
    TestMergeData buildComplexModelsForMerging(String folderId) {
        buildComplexTerminologyModelsForMerging(folderId)
    }

    TestMergeData buildComplexTerminologyModelsForMerging(String folderId) {
        String caTerminology = buildCommonAncestorTerminology(folderId)

        PUT("terminologies/$caTerminology/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response
        PUT("terminologies/$caTerminology/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("terminologies/$caTerminology/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        Map<String, Object> sourceMap = modifySourceTerminology(source)
        Map<String, Object> targetMap = modifyTargetTerminology(target)

        new TestMergeData(source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap
        )
    }

    TestMergeData buildComplexCodeSetModelsForMerging(String folderId) {
        String caTerminology = buildCommonAncestorTerminology(folderId)

        POST("terminologies/$caTerminology/terms", [code: 'ALO', definition: 'addLeftOnly'])
        verifyResponse CREATED, response
        POST("terminologies/$caTerminology/terms", [code: 'AAARD', definition: 'addAndAddReturningDifference'])
        verifyResponse CREATED, response

        PUT("terminologies/$caTerminology/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        String caCodeSetId = buildCommonAncestorCodeSet(folderId.toString(), caTerminology)
        PUT("codeSets/$caCodeSetId/finalise", [versionChangeType: 'Major'])
        verifyResponse OK, response

        PUT("codeSets/$caCodeSetId/newBranchModelVersion", [branchName: VersionAwareConstraints.DEFAULT_BRANCH_NAME])
        verifyResponse CREATED, response
        String target = responseBody().id
        PUT("codeSets/$caCodeSetId/newBranchModelVersion", [branchName: 'source'])
        verifyResponse CREATED, response
        String source = responseBody().id

        Map<String, Object> sourceMap = [:]
        Map<String, Object> targetMap = [:]

        sourceMap.codeSet = modifySourceCodeSet(source)
        targetMap.codeSet = modifyTargetCodeSet(target)

        new TestMergeData(source: source,
                          target: target,
                          sourceMap: sourceMap,
                          targetMap: targetMap,
                          otherMap: [
                              caTerminology: caTerminology,
                          ])
    }

    String buildCommonAncestorTerminology(String folderId) {
        POST("folders/${folderId}/terminologies", [
            label: 'Functional Test Terminology 1'
        ])
        verifyResponse(CREATED, response)
        String baseTerminology = responseBody().id

        POST("terminologies/$baseTerminology/terms", [code: 'DLO', definition: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'MLO', definition: 'modifyLeftOnly'])
        verifyResponse CREATED, response
        String modifyLeftOnly = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'SMLO', definition: 'secondModifyLeftOnly'])
        verifyResponse CREATED, response
        String secondModifyLeftOnly = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'DAM', definition: 'deleteAndModify'])
        verifyResponse CREATED, response
        String deleteAndModify = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'MAD', definition: 'modifyAndDelete'])
        verifyResponse CREATED, response
        String modifyAndDelete = responseBody().id
        POST("terminologies/$baseTerminology/terms", [code: 'MAMRD', definition: 'modifyAndModifyReturningDifference'])
        verifyResponse CREATED, response
        String modifyAndModifyReturningDifference = responseBody().id

        POST("terminologies/$baseTerminology/termRelationshipTypes", [label: 'inverseOf'])
        verifyResponse CREATED, response
        String inverseOf = responseBody().id
        POST("terminologies/$baseTerminology/termRelationshipTypes", [label: 'oppositeActionTo'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/termRelationshipTypes", [label: 'similarSourceAction'])
        verifyResponse CREATED, response
        String similarSourceAction = responseBody().id
        POST("terminologies/$baseTerminology/termRelationshipTypes", [label: 'sameSourceActionType'])
        verifyResponse CREATED, response
        String sameSourceActionType = responseBody().id

        POST("terminologies/$baseTerminology/terms/$deleteAndModify/termRelationships", [
            targetTerm      : modifyAndDelete,
            relationshipType: inverseOf,
            sourceTerm      : deleteAndModify
        ])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms/$modifyLeftOnly/termRelationships", [
            targetTerm      : modifyAndModifyReturningDifference,
            relationshipType: similarSourceAction,
            sourceTerm      : modifyLeftOnly
        ])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms/$secondModifyLeftOnly/termRelationships", [
            targetTerm      : modifyLeftOnly,
            relationshipType: sameSourceActionType,
            sourceTerm      : secondModifyLeftOnly
        ])
        verifyResponse CREATED, response


        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        verifyResponse CREATED, response


        baseTerminology
    }

    Map modifySourceTerminology(String source) {
        // Modify Source
        Map sourceMap = [
            terminologyId                             : getIdFromPath(source, 'te:Functional Test Terminology 1$source'),
            deleteLeftOnly                            : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:DLO'),
            modifyLeftOnly                            : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MLO'),
            secondModifyLeftOnly                      : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:SMLO'),
            deleteAndModify                           : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:DAM'),
            modifyAndDelete                           : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MAD'),
            modifyAndModifyReturningDifference        : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tm:MAMRD'),
            oppositeActionTo                          : getIdFromPath(source, 'te:Functional Test Terminology 1$source|trt:oppositeActionTo'),
            inverseOf                                 : getIdFromPath(source, 'te:Functional Test Terminology 1$source|trt:inverseOf'),
            sameSourceActionType                      : getIdFromPath(source, 'te:Functional Test Terminology 1$source|trt:sameSourceActionType'),
            similarSourceAction                       : getIdFromPath(source, 'te:Functional Test Terminology 1$source|trt:similarSourceAction'),
            similarSourceActionOnModifyLeftOnly       : getIdFromPath(source, 'te:Functional Test Terminology 1$source|tr:MLO.similarSourceAction.MAMRD'),
            sameSourceActionTypeOnSecondModifyLeftOnly: getIdFromPath(source, 'te:Functional Test Terminology 1$source|tr:SMLO.sameSourceActionType.MLO'),
            metadataModifyOnSource                    : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource                  : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete                   : getIdFromPath(source, 'te:Functional Test Terminology 1$source|md:functional.test.modifyAndDelete'),
        ]

        DELETE("terminologies/$sourceMap.terminologyId/terms/$sourceMap.deleteLeftOnly")
        verifyResponse NO_CONTENT, response
        DELETE("terminologies/$sourceMap.terminologyId/terms/$sourceMap.deleteAndModify")
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyLeftOnly", [description: 'Description'])
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyAndDelete", [description: 'Description'])
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyAndModifyReturningDifference", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/terms", [code: 'ALO', definition: 'addLeftOnly'])
        verifyResponse CREATED, response
        sourceMap.addLeftOnly = responseBody().id
        POST("terminologies/$sourceMap.terminologyId/terms", [code: 'SALO', definition: 'secondAddLeftOnly'])
        verifyResponse CREATED, response
        sourceMap.secondAddLeftOnly = responseBody().id

        POST("terminologies/$sourceMap.terminologyId/terms", [
            code       : 'AAARD',
            definition : 'addAndAddReturningDifference',
            description: 'DescriptionLeft'])
        verifyResponse CREATED, response
        sourceMap.addAndAddReturningDifference = responseBody().id

        DELETE("terminologies/$sourceMap.terminologyId/termRelationshipTypes/$sourceMap.oppositeActionTo")
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$sourceMap.terminologyId/termRelationshipTypes/$sourceMap.inverseOf", [description: 'inverseOf(Modified)'])
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/termRelationshipTypes", [label: 'sameActionAs'])
        verifyResponse CREATED, response
        sourceMap.sameActionAs = responseBody().id

        //termRelationships
        DELETE("terminologies/$sourceMap.terminologyId/terms/$sourceMap.modifyLeftOnly/termRelationships/$sourceMap.similarSourceActionOnModifyLeftOnly")
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$sourceMap.terminologyId/terms/$sourceMap.secondModifyLeftOnly/termRelationships/$sourceMap.sameSourceActionTypeOnSecondModifyLeftOnly",
            [description: 'NewDescription'])
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/terms/$sourceMap.addLeftOnly/termRelationships", [
            targetTerm      : sourceMap.addAndAddReturningDifference,
            relationshipType: sourceMap.similarSourceAction,
            sourceTerm      : sourceMap.addLeftOnly
        ])
        verifyResponse CREATED, response
        sourceMap.similarSourceActionOnAddLeftOnly = responseBody().id

        POST("terminologies/$sourceMap.terminologyId/terms/$sourceMap.addLeftOnly/termRelationships", [
            targetTerm      : sourceMap.secondAddLeftOnly,
            relationshipType: sourceMap.sameSourceActionType,
            sourceTerm      : sourceMap.addLeftOnly
        ])
        verifyResponse CREATED, response
        sourceMap.sameSourceActionTypeOnAddLeftOnly = responseBody().id


        PUT("terminologies/$sourceMap.terminologyId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        POST("terminologies/$sourceMap.terminologyId/metadata",
             [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        verifyResponse CREATED, response
        PUT("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        verifyResponse OK, response
        PUT("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        verifyResponse OK, response
        DELETE("terminologies/$sourceMap.terminologyId/metadata/$sourceMap.metadataDeleteFromSource")
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetTerminology(String target) {
        // Modify Target
        Map targetMap = [
            terminologyId                             : getIdFromPath(target, 'te:Functional Test Terminology 1$main'),
            deleteLeftOnly                            : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:DLO'),
            modifyLeftOnly                            : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MLO'),
            secondModifyLeftOnly                      : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:SMLO'),
            deleteAndModify                           : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:DAM'),
            modifyAndDelete                           : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MAD'),
            modifyAndModifyReturningDifference        : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tm:MAMRD'),
            oppositeActionTo                          : getIdFromPath(target, 'te:Functional Test Terminology 1$main|trt:oppositeActionTo'),
            inverseOf                                 : getIdFromPath(target, 'te:Functional Test Terminology 1$main|trt:inverseOf'),
            sameSourceActionType                      : getIdFromPath(target, 'te:Functional Test Terminology 1$main|trt:sameSourceActionType'),
            similarSourceAction                       : getIdFromPath(target, 'te:Functional Test Terminology 1$main|trt:similarSourceAction'),
            similarSourceActionOnModifyLeftOnly       : getIdFromPath(target, 'te:Functional Test Terminology 1$main|tr:MLO.similarSourceAction.MAMRD'),
            sameSourceActionTypeOnSecondModifyLeftOnly: getIdFromPath(target, 'te:Functional Test Terminology 1$main|tr:SMLO.sameSourceActionType.MLO'),
            metadataModifyOnSource                    : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.modifyOnSource'),
            metadataDeleteFromSource                  : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.deleteFromSource'),
            metadataModifyAndDelete                   : getIdFromPath(target, 'te:Functional Test Terminology 1$main|md:functional.test.modifyAndDelete'),
        ]

        DELETE("terminologies/$targetMap.terminologyId/terms/$targetMap.modifyAndDelete")
        verifyResponse NO_CONTENT, response

        PUT("terminologies/$targetMap.terminologyId/terms/$targetMap.deleteAndModify", [description: 'Description'])
        verifyResponse OK, response
        PUT("terminologies/$targetMap.terminologyId/terms/$targetMap.modifyAndModifyReturningDifference", [description: 'DescriptionRight'])
        verifyResponse OK, response

        POST("terminologies/$targetMap.terminologyId/terms", [code: 'ARO', definition: 'addRightOnly'])
        verifyResponse CREATED, response
        POST("terminologies/$targetMap.terminologyId/terms", [code: 'AAARD', definition: 'addAndAddReturningDifference', description: 'DescriptionRight'])
        verifyResponse CREATED, response
        targetMap.addAndAddReturningDifference = responseBody().id

        PUT("terminologies/$targetMap.terminologyId", [description: 'DescriptionRight'])
        verifyResponse OK, response
        DELETE("terminologies/$targetMap.terminologyId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }

    String buildCommonAncestorCodeSet(String folderId, String terminologyId) {
        POST("folders/$folderId/codeSets", [
            label: 'Functional Test CodeSet 1'
        ])
        verifyResponse(CREATED, response)
        String codeSetId = responseBody().id
        Map caIds = [
            terminologyId                     : terminologyId,
            deleteLeftOnly                    : getIdFromPath(terminologyId, 'tm:DLO', 'terminologies'),
            modifyLeftOnly                    : getIdFromPath(terminologyId, 'tm:MLO', 'terminologies'),
            deleteAndModify                   : getIdFromPath(terminologyId, 'tm:DAM', 'terminologies'),
            modifyAndDelete                   : getIdFromPath(terminologyId, 'tm:MAD', 'terminologies'),
            modifyAndModifyReturningDifference: getIdFromPath(terminologyId, 'tm:MAMRD', 'terminologies'),
        ]
        PUT("codeSets/$codeSetId", [terms: [
            [id: caIds.deleteLeftOnly],
            [id: caIds.modifyLeftOnly],
            [id: caIds.deleteAndModify],
            [id: caIds.modifyAndDelete],
            [id: caIds.modifyAndModifyReturningDifference]
        ]])
        verifyResponse OK, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'nothingDifferent', value: 'this shouldnt change'])
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'modifyOnSource', value: 'some original value'])
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'deleteFromSource', value: 'some other original value'])
        verifyResponse CREATED, response
        POST("codeSets/$codeSetId/metadata", [namespace: 'functional.test', key: 'modifyAndDelete', value: 'some other original value 2'])
        verifyResponse CREATED, response
        codeSetId
    }

    Map modifySourceCodeSet(String source) {
        // Modify Source
        Map sourceMap = [
            codeSetId                         : source,
            deleteLeftOnly                    : getIdFromPath(source, 'tm:DLO'),
            modifyLeftOnly                    : getIdFromPath(source, 'tm:MLO'),
            modifyAndDelete                   : getIdFromPath(source, 'tm:MAD'),
            deleteAndModify                   : getIdFromPath(source, 'tm:DAM'),
            modifyAndModifyReturningDifference: getIdFromPath(source, 'tm:MAMRD'),
            addLeftOnly                       : getIdFromPath(source, 'te:Functional Test Terminology 1|tm:ALO'),
            addAndAddReturningDifference      : getIdFromPath(source, 'te:Functional Test Terminology 1|tm:AAARD'),
            metadataModifyOnSource            : getIdFromPath(source, 'md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(source, 'md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(source, 'md:functional.test.modifyAndDelete'),
        ]

        PUT("codeSets/$sourceMap.codeSetId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addLeftOnly", [:])
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addAndAddReturningDifference", [:])
        verifyResponse OK, response

        DELETE("codeSets/$sourceMap.codeSetId/terms/$sourceMap.deleteLeftOnly")
        verifyResponse OK, response
        DELETE("codeSets/$sourceMap.codeSetId/terms/$sourceMap.deleteAndModify")
        verifyResponse OK, response

        POST("codeSets/$sourceMap.codeSetId/metadata",
             [namespace: 'functional.test', key: 'addToSourceOnly', value: 'adding to source only'])
        verifyResponse CREATED, response
        PUT("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataModifyOnSource", [value: 'source has modified this'])
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataModifyAndDelete", [value: 'source has modified this also'])
        verifyResponse OK, response
        DELETE("codeSets/$sourceMap.codeSetId/metadata/$sourceMap.metadataDeleteFromSource")
        verifyResponse NO_CONTENT, response

        sourceMap
    }

    Map modifyTargetCodeSet(String target) {
        // Modify Target
        Map targetMap = [
            codeSetId                         : target,
            modifyLeftOnly                    : getIdFromPath(target, 'tm:MLO'),
            modifyAndDelete                   : getIdFromPath(target, 'tm:MAD'),
            modifyAndModifyReturningDifference: getIdFromPath(target, 'tm:MAMRD'),
            addAndAddReturningDifference      : getIdFromPath(target, 'te:Functional Test Terminology 1|tm:AAARD'),
            metadataModifyOnSource            : getIdFromPath(target, 'md:functional.test.modifyOnSource'),
            metadataDeleteFromSource          : getIdFromPath(target, 'md:functional.test.deleteFromSource'),
            metadataModifyAndDelete           : getIdFromPath(target, 'md:functional.test.modifyAndDelete'),
        ]
        PUT("codeSets/$targetMap.codeSetId/terms/$targetMap.addAndAddReturningDifference", [:])
        verifyResponse OK, response
        DELETE("codeSets/$targetMap.codeSetId/terms/$targetMap.modifyAndDelete")
        verifyResponse OK, response

        DELETE("codeSets/$targetMap.codeSetId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }

    String getIdFromPath(String rootResourceId, String path, String resourceDomainType) {
        GET("$resourceDomainType/$rootResourceId/path/${URLEncoder.encode(path, Charset.defaultCharset())}")
        verifyResponse OK, response
        assert responseBody().id
        responseBody().id
    }
}
