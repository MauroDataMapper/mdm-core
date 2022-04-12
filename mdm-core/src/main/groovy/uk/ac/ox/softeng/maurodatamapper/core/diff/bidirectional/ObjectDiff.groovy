/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional

import uk.ac.ox.softeng.maurodatamapper.core.api.exception.ApiDiffException
import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.orm.hibernate.proxy.HibernateProxyHandler

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.arrayDiff
import static uk.ac.ox.softeng.maurodatamapper.core.diff.DiffBuilder.fieldDiff

/*
Always in relation to the lhs
 */

@Slf4j
class ObjectDiff<O extends Diffable> extends BiDirectionalDiff<O> {

    private static final HibernateProxyHandler hibernateProxyHandler = new HibernateProxyHandler()

    DiffCache lhsDiffCache
    DiffCache rhsDiffCache

    List<FieldDiff> diffs

    String leftId
    String rightId
    boolean versionedDiff

    ObjectDiff(Class<O> targetClass) {
        this(targetClass, new DiffCache(), new DiffCache())
    }

    ObjectDiff(Class<O> targetClass, DiffCache lhsDiffCache, DiffCache rhsDiffCache) {
        super(targetClass)
        diffs = []
        this.lhsDiffCache = lhsDiffCache
        this.rhsDiffCache = rhsDiffCache
    }

    @Override
    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false
        if (!super.equals(o)) return false

        ObjectDiff<O> objectDiff = (ObjectDiff<O>) o

        if (leftId != objectDiff.leftId) return false
        if (rightId != objectDiff.rightId) return false
        diffs == objectDiff.diffs
    }

    @Override
    int hashCode() {
        int result = super.hashCode()
        result = 31 * result + (diffs != null ? diffs.hashCode() : 0)
        result = 31 * result + leftId.hashCode()
        result = 31 * result + rightId.hashCode()
        result
    }

    @Override
    String toString() {
        int numberOfDiffs = getNumberOfDiffs()
        if (!numberOfDiffs) return "${leftIdentifier} == ${rightIdentifier}"
        "${leftIdentifier} <> ${rightIdentifier} :: ${numberOfDiffs} differences\n  ${diffs.collect {it.toString()}.join('\n  ')}"
    }

    @Override
    Integer getNumberOfDiffs() {
        diffs?.sum {it.getNumberOfDiffs()} as Integer ?: 0
    }

    String getLeftIdentifier() {
        left.diffIdentifier
    }

    String getRightIdentifier() {
        right.diffIdentifier
    }

    boolean isVersionedDiff() {
        versionedDiff ?: Path.from(left.pathPrefix, left.pathIdentifier).first().modelIdentifier
    }

    ObjectDiff<O> leftHandSide(String leftId, O lhs) {
        super.leftHandSide(lhs)
        this.leftId = leftId
        this
    }

    ObjectDiff<O> rightHandSide(String rightId, O rhs) {
        rightHandSide(rhs)
        this.rightId = rightId
        this
    }

    ObjectDiff<O> withRightHandSideCache(DiffCache rhsDiffCache) {
       this.rhsDiffCache = rhsDiffCache
        this
    }

    ObjectDiff<O> withLeftHandSideCache(DiffCache lhsDiffCache) {
        this.lhsDiffCache = lhsDiffCache
        this
    }

    ObjectDiff<O> asVersionedDiff() {
        versionedDiff = true
        this
    }

    ObjectDiff<O> appendNumber(final String fieldName, final Number lhs, final Number rhs) throws ApiDiffException {
        append(fieldDiff(Number), fieldName, lhs, rhs)
    }

    ObjectDiff<O> appendBoolean(final String fieldName, final Boolean lhs, final Boolean rhs) throws ApiDiffException {
        append(fieldDiff(Boolean), fieldName, lhs, rhs)
    }

    ObjectDiff<O> appendString(final String fieldName, final String lhs, final String rhs) throws ApiDiffException {
        append(fieldDiff(String), fieldName, clean(lhs), clean(rhs))
    }

    ObjectDiff<O> appendOffsetDateTime(final String fieldName, final OffsetDateTime lhs, final OffsetDateTime rhs) throws ApiDiffException {
        append(fieldDiff(OffsetDateTime), fieldName, lhs, rhs)
    }

    def <K> ObjectDiff<O> append(FieldDiff<K> fieldDiff, String fieldName, K lhs, K rhs) {
        validateFieldNameNotNull(fieldName)
        if (lhs == null && rhs == null) {
            return this
        }
        if (lhs != rhs) {
            append(fieldDiff.fieldName(fieldName).leftHandSide(lhs).rightHandSide(rhs))
        }
        this
    }

    ObjectDiff<O> append(FieldDiff fieldDiff) {
        diffs.add(fieldDiff)
        this
    }

    FieldDiff find(@DelegatesTo(List) @ClosureParams(value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff') Closure closure) {
        diffs.find closure
    }

    @Deprecated
    def <K extends Diffable> ObjectDiff<O> appendList(Class<K> diffableClass, String fieldName,
                                                      Collection<K> appendingLhs, Collection<K> appendingRhs, String context = null,
                                                      boolean addIfEmpty = false) throws ApiDiffException {
        appendCollection(diffableClass, fieldName, appendingLhs, appendingRhs, context, addIfEmpty)
    }

    def <K extends Diffable> ObjectDiff<O> appendCollection(Class<K> diffableClass, String fieldName,
                                                            String context = null,
                                                            boolean addIfEmpty = false) throws ApiDiffException {

        Collection<K> lhs = lhsDiffCache.getCollection(fieldName, diffableClass)
        Collection<K> rhs = rhsDiffCache.getCollection(fieldName, diffableClass)
        appendCollection(diffableClass, fieldName, lhs, rhs, context, addIfEmpty, true)
    }

    def <K extends Diffable> ObjectDiff<O> appendCollection(Class<K> diffableClass, String fieldName,
                                                            Collection<K> appendingLhs, Collection<K> appendingRhs, String context = null,
                                                            boolean addIfEmpty = false) throws ApiDiffException {
        log.warn('Adding collection [{}] to [{}] without caching, this will be slower', fieldName, leftIdentifier)
        Collection<K> lhs
        Collection<K> rhs
        if (GormEntity.isAssignableFrom(diffableClass)) {
            lhs = appendingLhs.collect {hibernateProxyHandler.unwrapIfProxy(it)} as Collection<K>
            rhs = appendingRhs.collect {hibernateProxyHandler.unwrapIfProxy(it)} as Collection<K>
        } else {
            lhs = appendingLhs
            rhs = appendingRhs
        }

        appendCollection(diffableClass, fieldName, lhs, rhs, context, addIfEmpty, false)
    }

    def <K extends Diffable> ObjectDiff<O> appendCollection(Class<K> diffableClass, String fieldName,
                                                            Collection<K> lhs, Collection<K> rhs, String context,
                                                            boolean addIfEmpty, boolean caching) throws ApiDiffException {

        validateFieldNameNotNull(fieldName)

        List<K> diffableList = []

        ArrayDiff<K> diff = arrayDiff(diffableList.class)
            .fieldName(fieldName)
            .leftHandSide(lhs ?: [])
            .rightHandSide(rhs ?: []) as ArrayDiff<K>

        // If no lhs or rhs then nothing to compare
        if (!lhs && !rhs) return addIfEmpty ? append(diff) : this

        // If no lhs then all rhs have been created/added
        if (!lhs) {
            return append(diff.createdObjects(rhs))
        }

        // If no rhs then all lhs have been deleted/removed
        if (!rhs) {
            return append(diff.deletedObjects(lhs))
        }

        Collection<K> deleted = []
        Collection<ObjectDiff> modified = []

        // Assume all rhs have been created new
        List<K> created = new ArrayList<>(rhs)

        Map<String, K> lhsMap = lhs.collectEntries {[it.getDiffIdentifier(context), it]}
        Map<String, K> rhsMap = rhs.collectEntries {[it.getDiffIdentifier(context), it]}
        // This object diff is being performed on an object which has the concept of modelIdentifier, e.g branch name or version
        // If this is the case we want to make sure we ignore any versioning on sub contents as child versioning is controlled by the parent
        // This should only happen to models inside versioned folders, but we want to try and be more dynamic
        if (isVersionedDiff()) {
            Path childPath = Path.from((MdmDomain) lhs.first())
            if (childPath.size() == 1 && childPath.first().modelIdentifier) {
                // child collection has versioning
                // recollect entries using the clean identifier rather than the full thing
                lhsMap = lhs.collectEntries {[Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it]}
                rhsMap = rhs.collectEntries {[Path.from(it.pathPrefix, it.getDiffIdentifier(context)).first().identifier, it]}
            }
        }

        // Work through each lhs object and compare to rhs object
        lhsMap.each {di, lObj ->
            K rObj = rhsMap[di]
            if (rObj) {
                // If robj then it exists and has not been created
                created.remove(rObj)
                ObjectDiff od = caching ?
                                lObj.diff(rObj, context, lhsDiffCache.getDiffCache(lObj.getPath()), rhsDiffCache.getDiffCache(rObj.getPath())) :
                                lObj.diff(rObj, context, null, null)
                // If not equal then objects have been modified
                if (!od.objectsAreIdentical()) {
                    modified.add(od)
                }
            } else {
                // If no robj then object has been deleted from lhs
                deleted.add(lObj)
            }
        }

        if (created || deleted || modified || addIfEmpty) {
            append(diff.createdObjects(created)
                       .deletedObjects(deleted)
                       .withModifiedDiffs(modified))
        }
        this

    }

    static void validateFieldNameNotNull(final String fieldName) throws ApiDiffException {
        if (!fieldName) {
            throw new ApiDiffException('OD01', 'Field name cannot be null or blank')
        }
    }

    static String clean(String s) {
        s?.trim() ?: null
    }
}

