/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.test.unit.core

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ArrayDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

/**
 * @since 21/09/2017
 */
@Slf4j
abstract class CatalogueItemSpec<K extends CatalogueItem> extends CreatorAwareSpec<K> {

    abstract K createValidDomain(String label)

    @Override
    void setValidDomainValues() {
        super.setValidDomainValues()
        domain.label = 'test'
    }

    @Override
    void verifyDomainConstraints(K domain) {
        super.verifyDomainConstraints(domain)
        domain.label == 'test'
    }

    def setup() {
        log.debug('Setting up CatalogueItem unit')
        mockDomains(Classifier, Folder, Annotation, BreadcrumbTree, Edit, Metadata, ReferenceFile, SemanticLink, Authority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        checkAndSave(new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST))
    }

    Folder getTestFolder() {
        Folder.findByLabel('catalogue')
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }

    int getExpectedBaseLevelOfDiffs() {
        0
    }

    void wipeBasicConstrained() {
        domain.label = null
        domain.aliasesString = null
    }

    int getExpectedConstrainedErrors() {
        1 // label
    }

    void setBasicConstrainedBlank() {
        domain.label = ''
        domain.description = ''
        domain.aliasesString = ''
    }

    int getExpectedConstrainedBlankErrors() {
        4
    }

    String getExpectedNewlineLabel() {
        'new label'
    }

    void verifyBlankConstraints() {
        assert domain.errors.getFieldError('label').code == 'blank'
        assert domain.errors.getFieldError('description').code == 'blank'
        assert domain.errors.getFieldError('aliasesString').code == 'blank'
        // #4 == breadcrumbtree.label
    }

    void 'CI01 : test constrained properties'() {
        given:
        setValidDomainValues()
        wipeBasicConstrained()

        assert !domain.metadata
        assert !domain.classifiers
        assert !domain.edits
        assert !domain.annotations
        assert !domain.semanticLinks
        assert !domain.referenceFiles

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == expectedConstrainedErrors
        domain.errors.getFieldError('label').code == 'nullable'

        when:
        setBasicConstrainedBlank()
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == expectedConstrainedBlankErrors
        verifyBlankConstraints()
    }

    void 'CI02 : test metadata addition'() {

        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.addToMetadata(namespace: 'http://test.com', key: 'testmd', value: 'a value', createdBy: admin.emailAddress)

        then:
        checkAndSave(domain)
        domain.count() == 1
        Metadata.count() == 1

        when:
        item = findById()
        Metadata md = Metadata.findByNamespaceAndKey('http://test.com', 'testmd')

        then:
        md
        item.metadata.size() == 1

        and:
        domain.findMetadataByNamespace('http://test.com').size() == 1
        domain.findMetadataByNamespaceAndKey('http://test.com', 'testmd')?.value == 'a value'

        when: 'manually adding another with the same NS and key'
        Metadata addMe = new Metadata(namespace: 'http://test.com', key: 'testmd', value: 'another value', createdBy: admin.emailAddress)
        domain.metadata << addMe
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('metadata').code == 'invalid.unique.values.message'
        Metadata.count() == 1

        when:
        md = Metadata.findByNamespaceAndKey('http://test.com', 'testmd')

        then:
        md
        md.value == 'a value'

        when: 'properly adding another with the same NS and key'
        domain.metadata.remove(addMe)
        domain.addToMetadata(addMe)

        then:
        checkAndSave(domain)
        checkAndSave(domain.metadata[0])
        domain.count() == 1
        Metadata.count() == 1

        when:
        md = Metadata.findByNamespaceAndKey('http://test.com', 'testmd')

        then:
        md
        md.value == 'another value'
    }

    void 'CI03 : test metadata updating'() {

        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.addToMetadata(namespace: 'http://test.com', key: 'testmd', value: 'a value', createdBy: admin.emailAddress)

        then:
        checkAndSave(domain)
        domain.count() == 1
        Metadata.count() == 1

        when:
        item = findById()
        Metadata md = Metadata.findByNamespaceAndKey('http://test.com', 'testmd')

        then:
        md
        item.metadata.size() == 1

        and:
        item.findMetadataByNamespace('http://test.com').size() == 1
        item.findMetadataByNamespaceAndKey('http://test.com', 'testmd')?.value == 'a value'

        when:
        domain.addToMetadata(namespace: 'http://test.com', key: 'testmd', value: 'a different value', createdBy: admin.emailAddress)

        then:
        checkAndSave(domain)
        domain.metadata.each {checkAndSave(it)}
        domain.count() == 1
        Metadata.count() == 1

        when:
        item = findById()
        md = Metadata.findByNamespaceAndKey('http://test.com', 'testmd')

        then:
        md.value == 'a different value'

        and:
        item.metadata.size() == 1
        item.findMetadataByNamespace('http://test.com').size() == 1
        item.findMetadataByNamespaceAndKey('http://test.com', 'testmd')?.value == 'a different value'

        when:
        domain.addToMetadata(namespace: 'http://test.com', key: 'testmd2', value: 'a different value2', createdBy: admin.emailAddress)

        then:
        checkAndSave(domain)
        domain.count() == 1
        Metadata.count() == 2

        and:
        domain.findMetadataByNamespace('http://test.com').size() == 2
        domain.findMetadataByNamespaceAndKey('http://test.com', 'testmd2')?.value == 'a different value2'
    }

    void 'CI04 : test adding annotations'() {
        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation')

        then:
        checkAndSave(domain)
        domain.count() == 1
        Annotation.count() == 1

        when:
        item = findById()
        Annotation ann = Annotation.findByLabel('test annotation')

        then:
        ann
        item.annotations.size() == 1

        and:
        ann.multiFacetAwareItemId == item.id
        ann.path == ''
    }

    void 'CI05 : test adding classifiers'() {
        given:
        setValidDomainValues()
        def second = createValidDomain('other')

        expect:
        checkAndSave(second)

        when:
        Classifier classifier = new Classifier(createdBy: editor.emailAddress, label: 'test classifier')
        domain.addToClassifiers(classifier)
        domain.addToClassifiers(createdBy: editor.emailAddress, label: 'test classifier2')
        second.addToClassifiers(classifier)

        then: 'no cascading in many-to-many so we need to save the classifiers manually and this must be done first'
        domain.classifiers.each {checkAndSave(it)}

        and:
        checkAndSave(domain)
        checkAndSave(second)

        and:
        domain.count() == 2
        Classifier.count() == 2

        when:
        item = findById()
        Classifier cl = Classifier.findByLabel('test classifier')

        then:
        cl
        item.classifiers.size() == 2
    }

    void 'CI06 : test adding aliases'() {
        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.aliases = []

        then:
        check(domain)
        domain.aliasesString == null
        domain.aliases.isEmpty()

        when:
        domain.aliases = ['hello']

        then:
        check(domain)
        domain.aliasesString == 'hello'
        domain.aliases.size() == 1
        domain.aliases.first() == 'hello'

        when:
        domain.aliases = ['hello', 'test2']

        then:
        check(domain)
        domain.aliasesString == 'hello|test2'
        domain.aliases.size() == 2

        when:
        domain.aliases += 'bye'

        then:
        check(domain)
        domain.aliasesString == 'hello|test2|bye'
        domain.aliases.size() == 3
    }

    void 'CI07 : test addto semantic links'() {
        given:
        K target = createValidDomain('test target')
        setValidDomainValues()

        expect:
        checkAndSave(target)
        checkAndSave(domain)

        when:
        domain.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: admin.emailAddress, targetMultiFacetAwareItem: target)

        then:
        checkAndSave(domain)
        domain.count() == 2
        SemanticLink.count() == 1

        when:
        item = findById()
        SemanticLink link_item = SemanticLink.byMultiFacetAwareItemId(domain.id).get()

        then:
        link_item

        and:
        link_item.linkType == SemanticLinkType.REFINES
        link_item.createdBy == admin.emailAddress
        item.semanticLinks.size() == 1
        item.semanticLinks.find {it.id == link_item.id}
    }

    void 'CI08 : test addto semantic links using same target and source'() {
        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdBy: admin.emailAddress, targetMultiFacetAwareItem: domain)
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.getFieldError('semanticLinks[0].multiFacetAwareItemId').code == 'invalid.same.property.message'
    }

    void 'CI09 : test add reference files'() {
        given:
        setValidDomainValues()

        expect:
        checkAndSave(domain)

        when:
        domain.addToReferenceFiles(fileName: 'test', fileType: MimeType.XML.toString(), fileContents: 'some content'.bytes,
                                   createdBy: editor.emailAddress)

        then:
        check(domain)
    }

    void 'CI10 : test diffing same object'() {
        given:
        setValidDomainValues()

        when:
        ObjectDiff diff = domain.diff(domain, null)

        then:
        noExceptionThrown()

        and:
        diff.objectsAreIdentical()
    }

    void 'CI11 : test diffing label'() {
        given:
        setValidDomainValues()
        K other = createValidDomain('another item')

        when:
        ObjectDiff diff = domain.diff(other, null)

        then:
        noExceptionThrown()

        and:
        diff.getNumberOfDiffs() == expectedBaseLevelOfDiffs + 1
        diff.diffs[0].fieldName == 'label'
        diff.diffs[0].left == domain.label
        diff.diffs[0].right == other.label
    }

    void 'CI12 : test diffing metadata'() {
        given:
        setValidDomainValues()
        K other = createValidDomain(domain.label)
        domain.addToMetadata('ns1', 'k1', 'v1')
        other.addToMetadata('ns1', 'k1', 'v1')

        when:
        ObjectDiff diff = domain.diff(other, null)

        then:
        noExceptionThrown()

        and:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs

        when:
        domain.addToMetadata('ns1', 'k1', 'v1')
        other.addToMetadata('ns1', 'k1', 'v2')
        diff = domain.diff(other, null)

        then:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs + 1

        when:
        ArrayDiff mdDiffs = diff.find { it.fieldName == 'metadata' } as ArrayDiff

        then:
        mdDiffs.getNumberOfDiffs() == 1
        mdDiffs.modified.size() == 1
        mdDiffs.modified[0].find { it.fieldName == 'value' }.left == 'v1'
        mdDiffs.modified[0].find { it.fieldName == 'value' }.right == 'v2'

        when:
        domain.addToMetadata('ns1', 'k2', 'v1')
        diff = domain.diff(other, null)

        then:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs + 2

        when:
        mdDiffs = diff.find { it.fieldName == 'metadata' } as ArrayDiff

        then:
        mdDiffs.getNumberOfDiffs() == 2
        mdDiffs.deleted.size() == 1
        mdDiffs.deleted[0].value.key == 'k2'
    }

    void 'CI13 : test diffing annotations'() {
        given:
        setValidDomainValues()
        K other = createValidDomain(domain.label)
        domain.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation')
        other.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation')

        when:
        ObjectDiff diff = domain.diff(other, null)

        then:
        noExceptionThrown()

        and:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs

        when:
        domain.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation 1')
        other.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation 1', description: 'hello')
        diff = domain.diff(other, null)

        then:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs + 1

        when:
        ArrayDiff diffs = diff.find { it.fieldName == 'annotations' } as ArrayDiff

        then:
        diffs.getNumberOfDiffs() == 1
        diffs.modified.size() == 1
        !diffs.modified[0].find { it.fieldName == 'description' }.left
        diffs.modified[0].find { it.fieldName == 'description' }.right == 'hello'

        when:
        domain.addToAnnotations(createdBy: editor.emailAddress, label: 'test annotation 3')
        diff = domain.diff(other, null)

        then:
        diff.numberOfDiffs == expectedBaseLevelOfDiffs + 2

        when:
        diffs = diff.find { it.fieldName == 'annotations' } as ArrayDiff

        then:
        diffs.getNumberOfDiffs() == 2
        diffs.deleted.size() == 1
        diffs.deleted[0].value.label == 'test annotation 3'
    }

    void 'CI14 : test newline in label'() {

        given:
        setValidDomainValues()
        domain.label = 'new\nlabel'


        when:
        checkAndSave(domain)

        then:
        domain.label == getExpectedNewlineLabel()
    }
}
