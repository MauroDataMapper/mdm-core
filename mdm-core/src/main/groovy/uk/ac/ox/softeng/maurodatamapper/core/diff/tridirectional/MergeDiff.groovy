/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import java.util.function.Predicate

/**
 * Holds the  result of 2 diffs which provides a unified diff of the changes which one object has made which another object has not made
 * based off a common ancestor
 *
 * The final diff should be the intent of the changes to merge the LHS/source INTO the LHS/target
 *
 * See <a href="https://metadatacatalogue.myjetbrains.com/youtrack/issue/MC-9228">YouTrack MC-9228</a> for rules
 *
 * <pre>
 *  source                                  target
 *    ^                                     ^
 *     \                                   /
 *   caSourceObjectDiff / source     caTargetObjectDiff / target
 *           \                           /
 *            \                         /
 *                  commonAncestor
 * </pre>
 */
@Slf4j
@CompileStatic
class MergeDiff<M extends Diffable> extends TriDirectionalDiff<M> implements Comparable<MergeDiff> {

    private List<FieldMergeDiff> diffs

    private ObjectDiff<M> commonAncestorDiffSource
    private ObjectDiff<M> commonAncestorDiffTarget
    private ObjectDiff<M> sourceDiffTarget

    List<TriDirectionalDiff> flattenedDiffs

    MergeDiff(Class<M> targetClass) {
        super(targetClass)
        diffs = []
    }

    @Override
    Boolean isMergeConflict() {
        !objectsAreIdentical()
    }

    @Override
    Integer getNumberOfDiffs() {
        flattenedDiffs ? flattenedDiffs.size() : diffs?.sum {it.getNumberOfDiffs()} as Integer ?: 0
    }

    String getSourceIdentifier() {
        source.diffIdentifier
    }

    String getTargetIdentifier() {
        target.diffIdentifier
    }

    String getSourceId() {
        (source as MdmDomain).id
    }

    String getTargetId() {
        (target as MdmDomain).id
    }

    Path getFullyQualifiedPath() {
        fullyQualifiedObjectPath ? fullyQualifiedObjectPath.resolve(source.path) : source.path
    }

    FieldMergeDiff first() {
        diffs.first()
    }

    int size() {
        diffs.size()
    }

    boolean isEmpty() {
        diffs.isEmpty()
    }

    List<FieldMergeDiff> getDiffs() {
        diffs.sort()
    }

    List<TriDirectionalDiff> getFlattenedDiffs() {
        if (!flattenedDiffs) flatten()
        flattenedDiffs
    }

    @Override
    String toString() {
        String str = "${sourceIdentifier} --> ${targetIdentifier}"
        if (commonAncestor) str = "${str} [${commonAncestor.diffIdentifier}]"
        str
    }

    MergeDiff<M> forMergingDiffable(M sourceSide) {
        super.withSource(sourceSide) as MergeDiff<M>
    }

    @Override
    MergeDiff<M> insideFullyQualifiedObjectPath(Path fullyQualifiedObjectPath) {
        super.insideFullyQualifiedObjectPath(fullyQualifiedObjectPath) as MergeDiff<M>
    }

    MergeDiff<M> intoDiffable(M targetSide) {
        super.withTarget(targetSide) as MergeDiff<M>
    }

    MergeDiff<M> havingCommonAncestor(M ca) {
        super.withCommonAncestor(ca) as MergeDiff<M>
    }

    MergeDiff<M> withCommonAncestorDiffedAgainstSource(ObjectDiff<M> commonAncestorDiffSource) {
        this.commonAncestorDiffSource = commonAncestorDiffSource
        this
    }

    MergeDiff<M> withCommonAncestorDiffedAgainstTarget(ObjectDiff<M> commonAncestorDiffTarget) {
        this.commonAncestorDiffTarget = commonAncestorDiffTarget
        this
    }

    MergeDiff<M> withSourceDiffedAgainstTarget(ObjectDiff<M> sourceDiffTarget) {
        this.sourceDiffTarget = sourceDiffTarget
        this
    }

    MergeDiff<M> append(FieldMergeDiff fieldDiff) {
        if (fieldDiff) diffs.add(fieldDiff)
        this
    }

    MergeDiff<M> asMergeConflict() {
        super.asMergeConflict() as MergeDiff<M>
    }

    MergeDiff<M> flatten() {
        flattenedDiffs = diffs.sort().collectMany {diff ->
            if (diff.diffType == FieldMergeDiff.simpleName) return [diff]
            if (diff.diffType == ArrayMergeDiff.simpleName) {
                return (diff as ArrayMergeDiff).getFlattenedDiffs()
            }
            []
        } as List<TriDirectionalDiff>
        this
    }

    MergeDiff<M> clean(@DelegatesTo(Predicate) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.TriDirectionalDiff') Closure removeIfTest) {
        flattenedDiffs.removeIf([test: removeIfTest,] as Predicate)
        this
    }

    FieldMergeDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.tridirectional.FieldMergeDiff') Closure closure) {
        diffs.find closure
    }

    @Override
    int compareTo(MergeDiff that) {
        this.sourceIdentifier <=> that.sourceIdentifier
    }

    ObjectDiff<M> getCommonAncestorDiffSource() {
        return commonAncestorDiffSource
    }

    ObjectDiff<M> getCommonAncestorDiffTarget() {
        return commonAncestorDiffTarget
    }

    ObjectDiff<M> getSourceDiffTarget() {
        return sourceDiffTarget
    }

    DiffCache getCommonAncestorDiffCache(){
        commonAncestorDiffSource.lhsDiffCache
    }

    DiffCache getSourceDiffCache(){
        commonAncestorDiffSource.rhsDiffCache
    }

    DiffCache getTargetDiffCache(){
        commonAncestorDiffTarget.rhsDiffCache
    }
}
