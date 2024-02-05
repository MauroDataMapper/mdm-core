/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.test.functional.merge

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.test.functional.merge.BaseTestMergeBuilder
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional

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

        // The 2 xxCS terms are for demonstrating changes to the CS when no changes at the Terminology or Term level
        // They are Terms which exist in all branches of the Terminology but are added or removed to the CS

        POST("terminologies/$baseTerminology/terms", [code: 'DLO', definition: 'deleteLeftOnly'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'DLOCS', definition: 'deleteLeftOnlyCodeSet'])
        verifyResponse CREATED, response
        POST("terminologies/$baseTerminology/terms", [code: 'ALOCS', definition: 'addLeftOnlyCodeSet'])
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
        POST("terminologies/$baseTerminology/termRelationshipTypes", [label: 'parentTo', parentalRelationship: true])
        verifyResponse CREATED, response
        String parentTo = responseBody().id

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
        POST("terminologies/$baseTerminology/terms/$secondModifyLeftOnly/termRelationships", [
            targetTerm      : modifyAndModifyReturningDifference,
            relationshipType: parentTo,
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

    Map modifySourceTerminology(String source, String pathing = '') {
        // Modify Source
        Map sourceMap = [
            terminologyId                             : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source"),
            deleteLeftOnly                            : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:DLO"),
            modifyLeftOnly                            : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:MLO"),
            secondModifyLeftOnly                      : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:SMLO"),
            deleteAndModify                           : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:DAM"),
            modifyAndDelete                           : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:MAD"),
            modifyAndModifyReturningDifference        : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tm:MAMRD"),
            oppositeActionTo                          : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|trt:oppositeActionTo"),
            inverseOf                                 : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|trt:inverseOf"),
            sameSourceActionType                      : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|trt:sameSourceActionType"),
            similarSourceAction                       : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|trt:similarSourceAction"),
            similarSourceActionOnModifyLeftOnly       : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tr:MLO.similarSourceAction.MAMRD"),
            sameSourceActionTypeOnSecondModifyLeftOnly: getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|tr:SMLO.sameSourceActionType.MLO"),
            metadataModifyOnSource                    : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource                  : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete                   : getIdFromPath(source, "${pathing}te:Functional Test Terminology 1\$source|md:functional.test.modifyAndDelete"),
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

    Map modifyTargetTerminology(String target, String pathing = '') {
        // Modify Target
        Map targetMap = [
            terminologyId                             : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main"),
            deleteLeftOnly                            : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:DLO"),
            modifyLeftOnly                            : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:MLO"),
            secondModifyLeftOnly                      : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:SMLO"),
            deleteAndModify                           : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:DAM"),
            modifyAndDelete                           : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:MAD"),
            modifyAndModifyReturningDifference        : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tm:MAMRD"),
            oppositeActionTo                          : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|trt:oppositeActionTo"),
            inverseOf                                 : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|trt:inverseOf"),
            sameSourceActionType                      : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|trt:sameSourceActionType"),
            similarSourceAction                       : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|trt:similarSourceAction"),
            similarSourceActionOnModifyLeftOnly       : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tr:MLO.similarSourceAction.MAMRD"),
            sameSourceActionTypeOnSecondModifyLeftOnly: getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|tr:SMLO.sameSourceActionType.MLO"),
            metadataModifyOnSource                    : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource                  : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete                   : getIdFromPath(target, "${pathing}te:Functional Test Terminology 1\$main|md:functional.test.modifyAndDelete"),
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
            deleteLeftOnlyCodeSet             : getIdFromPath(terminologyId, 'tm:DLOCS', 'terminologies'),
            modifyLeftOnly                    : getIdFromPath(terminologyId, 'tm:MLO', 'terminologies'),
            deleteAndModify                   : getIdFromPath(terminologyId, 'tm:DAM', 'terminologies'),
            modifyAndDelete                   : getIdFromPath(terminologyId, 'tm:MAD', 'terminologies'),
            modifyAndModifyReturningDifference: getIdFromPath(terminologyId, 'tm:MAMRD', 'terminologies'),
        ]
        PUT("codeSets/$codeSetId", [terms: [
            [id: caIds.deleteLeftOnly],
            [id: caIds.deleteLeftOnlyCodeSet],
            [id: caIds.modifyLeftOnly],
            [id: caIds.deleteAndModify],
            [id: caIds.modifyAndDelete],
            [id: caIds.modifyAndModifyReturningDifference],
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

    @Transactional
    Map modifySourceCodeSet(String source, String terminologyBranch = '$1.0.0', boolean terminologyAlreadyModified = false, String pathing = '',
                            String terminologyPathing = '') {
        // Modify Source
        Map sourceMap = [
            codeSetId                         : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source"),
            deleteLeftOnly                    : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:DLO", !terminologyAlreadyModified),
            deleteLeftOnlyCodeSet             : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:DLOCS"),
            modifyLeftOnly                    : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:MLO"),
            modifyAndDelete                   : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:MAD"),
            deleteAndModify                   : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:DAM", !terminologyAlreadyModified),
            modifyAndModifyReturningDifference: getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|tm:MAMRD"),
            addLeftOnly                       : getIdFromPath(source, "${terminologyPathing}te:Functional Test Terminology 1${terminologyBranch}|tm:ALO"),
            addLeftOnlyCodeSet                : getIdFromPath(source, "${terminologyPathing}te:Functional Test Terminology 1${terminologyBranch}|tm:ALOCS"),
            addAndAddReturningDifference      : getIdFromPath(source, "${terminologyPathing}te:Functional Test Terminology 1${terminologyBranch}|tm:AAARD"),
            metadataModifyOnSource            : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource          : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete           : getIdFromPath(source, "${pathing}cs:Functional Test CodeSet 1\$source|md:functional.test.modifyAndDelete"),
        ]

        PUT("codeSets/$sourceMap.codeSetId", [description: 'DescriptionLeft'])
        verifyResponse OK, response

        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addLeftOnly", [:])
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addAndAddReturningDifference", [:])
        verifyResponse OK, response
        PUT("codeSets/$sourceMap.codeSetId/terms/$sourceMap.addLeftOnlyCodeSet", [:])
        verifyResponse OK, response

        if (!terminologyAlreadyModified) {
            DELETE("codeSets/$sourceMap.codeSetId/terms/$sourceMap.deleteLeftOnly")
            verifyResponse OK, response
            DELETE("codeSets/$sourceMap.codeSetId/terms/$sourceMap.deleteAndModify")
            verifyResponse OK, response
        }
        DELETE("codeSets/$sourceMap.codeSetId/terms/$sourceMap.deleteLeftOnlyCodeSet")
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

    Map modifyTargetCodeSet(String target, String terminologyBranch = '$1.0.0', boolean terminologyAlreadyModified = false, String pathing = '',
                            String terminologyPathing = '') {
        // Modify Target
        Map targetMap = [
            codeSetId                         : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main"),
            modifyLeftOnly                    : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|tm:MLO"),
            modifyAndDelete                   : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|tm:MAD", !terminologyAlreadyModified),
            modifyAndModifyReturningDifference: getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|tm:MAMRD"),
            addAndAddReturningDifference      : getIdFromPath(target, "${terminologyPathing}te:Functional Test Terminology 1${terminologyBranch}|tm:AAARD"),
            metadataModifyOnSource            : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|md:functional.test.modifyOnSource"),
            metadataDeleteFromSource          : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|md:functional.test.deleteFromSource"),
            metadataModifyAndDelete           : getIdFromPath(target, "${pathing}cs:Functional Test CodeSet 1\$main|md:functional.test.modifyAndDelete"),
        ]
        PUT("codeSets/$targetMap.codeSetId/terms/$targetMap.addAndAddReturningDifference", [:])
        verifyResponse OK, response

        if (!terminologyAlreadyModified) {
            DELETE("codeSets/$targetMap.codeSetId/terms/$targetMap.modifyAndDelete")
            verifyResponse OK, response
        }

        DELETE("codeSets/$targetMap.codeSetId/metadata/$targetMap.metadataModifyAndDelete")
        verifyResponse NO_CONTENT, response

        targetMap
    }

    String getIdFromPath(String rootResourceId, String path, String resourceDomainType) {
        GET("$resourceDomainType/$rootResourceId/path/${Utils.safeUrlEncode(path)}")
        verifyResponse OK, response
        assert responseBody().id
        responseBody().id
    }
}
