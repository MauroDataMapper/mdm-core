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
package path

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.test.BaseTerminologyIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.Path
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature
import spock.lang.Stepwise

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Integration
@Rollback
@Stepwise
class TerminologyPathServiceSpec extends BaseTerminologyIntegrationSpec {

    PathService pathService

    Terminology terminology1
    Term terminology1_term1
    Term terminology1_term2

    Terminology terminology2
    Term terminology2_term1
    Term terminology2_term2

    Terminology terminology3main
    Terminology terminology3draft

    Terminology terminology4finalised
    Terminology terminology4notFinalised

    Terminology terminology5first
    Terminology terminology5second

    CodeSet codeSet1

    CodeSet codeSet2

    /*
    Set up test data like:

    "terminology 1"
           ->     "terminology 1 term 1"
           ->     "terminology 1 term 2"

    "terminology 2"
           ->     "terminology 2 term 1"
           ->     "terminology 2 term 2"

    "terminology 3" (branch "main")
    "terminology 3" (branch "draft")

    "terminology 4" (finalised)
    "terminology 4" (not finalised)

    "terminology 5" (finalised version 1.0.0)
    "terminology 5" (finalised version 2.0.0)        

    "code set 1"
           ->     "terminology 1 term 1"
           ->     "terminology 1 term 2"

    "code set 2"
           ->     "terminology 2 term 1"
           ->     "terminology 2 term 2"
     */

    void setupDomainData() {
        log.debug('Setting up TerminologyPathServiceSpec Unit')

        terminology1 = new Terminology(createdByUser: admin, label: 'terminology 1', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology1)
        terminology1_term1 = new Term(createdByUser: admin, code: 'c1', definition: 'terminology 1 term 1')
        terminology1.addToTerms(terminology1_term1)
        checkAndSave(terminology1)
        terminology1_term2 = new Term(createdByUser: admin, code: 'c2', definition: 'terminology 1 term 2')
        terminology1.addToTerms(terminology1_term2)
        checkAndSave(terminology1)

        terminology2 = new Terminology(createdByUser: admin, label: 'terminology 2', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology2)
        terminology2_term1 = new Term(createdByUser: admin, code: 'c3', definition: 'terminology 2 term 1')
        terminology2.addToTerms(terminology2_term1)
        checkAndSave(terminology2)
        terminology2_term2 = new Term(createdByUser: admin, code: 'c4', definition: 'terminology 2 term 2')
        terminology2.addToTerms(terminology2_term2)
        checkAndSave(terminology2)

        terminology3main = new Terminology(createdByUser: admin, label: 'terminology 3', description: 'terminology 3 on main', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology3main)

        terminology3draft = new Terminology(createdByUser: admin, label: 'terminology 3', description: 'terminology 3 on draft', folder: testFolder, authority: testAuthority,
                                            branchName: 'draft')
        checkAndSave(terminology3draft)

        terminology4finalised =
            new Terminology(createdByUser: admin, label: 'terminology 4', description: 'terminology 4 finalised', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology4finalised)
        terminology4finalised.finalised = true
        terminology4finalised.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology4finalised.breadcrumbTree.finalised = true
        terminology4finalised.modelVersion = Version.from('1.0.0')
        checkAndSave(terminology4finalised)

        terminology4notFinalised =
            new Terminology(createdByUser: admin, label: 'terminology 4', description: 'terminology 4 not finalised', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology4notFinalised)

        terminology5first = new Terminology(createdByUser: admin, label: 'terminology 5', description: 'terminology 5 1.0.0', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology5first)
        terminology5first.finalised = true
        terminology5first.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology5first.breadcrumbTree.finalised = true
        terminology5first.modelVersion = Version.from('1.0.0')
        checkAndSave(terminology5first)

        terminology5second = new Terminology(createdByUser: admin, label: 'terminology 5', description: 'terminology 5 2.0.0', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology5second)
        terminology5second.finalised = true
        terminology5second.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology5second.breadcrumbTree.finalised = true
        terminology5second.modelVersion = Version.from('2.0.0')
        checkAndSave(terminology5second)

        codeSet1 = new CodeSet(createdByUser: admin, label: 'codeset 1', folder: testFolder, authority: testAuthority)
        checkAndSave(codeSet1)
        codeSet1.addToTerms(terminology1_term1)
        checkAndSave(codeSet1)
        codeSet1.addToTerms(terminology1_term2)
        checkAndSave(codeSet1)

        codeSet2 = new CodeSet(createdByUser: admin, label: 'codeset 2', folder: testFolder, authority: testAuthority)
        checkAndSave(codeSet2)
        codeSet2.addToTerms(terminology2_term1)
        checkAndSave(codeSet2)
        codeSet2.addToTerms(terminology2_term2)
        checkAndSave(codeSet2)

    }

    void "test getting terminology by ID and path"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        Terminology 1 by ID
         */
        when:
        Path path = Path.from('te:')
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology1.id

        /*
        Terminology 2 by ID
        */
        when:
        path = Path.from('te:')
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology2.id

        /*
        Terminology 1 by ID and path
         */
        when:
        path = Path.from(terminology1)
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology1.id

        /*
        Terminology 2 by ID and path
         */
        when:
        path = Path.from(terminology2)
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology2.id

        /*
         Terminology 1 by ID and wrong path
        */
        when:
        path = Path.from(terminology2)
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        !catalogueItem

        /*
        Terminology 2 by ID and wrong path
         */
        when:
        path = Path.from(terminology1)
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        !catalogueItem

        /*
        Terminology 1 by path
        */
        when:
        path = Path.from(terminology1)
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology1.id

        /*
        Terminology 2 by path
        */
        when:
        path = Path.from(terminology2)
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology2.id
    }

    void "test getting terms for terminology"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        Terminology 1 Term 1
         */
        when:
        Path path = Path.from("te:|tm:${terminology1_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology1_term1.id

        /*
       Terminology 1 Term 1
        */
        when: 'using the label'
        path = Path.from("te:|tm:${terminology1_term1.label}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology1_term1.id

        /*
        Terminology 1 Term 2
        */
        when:
        path = Path.from("te:|tm:${terminology1_term2.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology1_term2.id

        /*
        Terminology 2 Term 1
        */
        when:
        path = Path.from("te:|tm:${terminology2_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology2_term1.id

        /*
        Terminology 2 Term 2
        */
        when:
        path = Path.from("te:|tm:${terminology2_term2.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology2_term2.id

        /*
        By path alone
         */
        when:
        path = Path.from(terminology2, terminology2_term2)
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology2_term2.id

        /*
        Try to get a term which doesn't exist on Terminology 1
        */
        when:
        path = Path.from("te:|tm:${terminology2_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology1, path) as CatalogueItem

        then:
        catalogueItem == null

        /*
        Try to get a term which doesn't exist on Terminology 2
        */
        when:
        path = Path.from("te:|tm:${terminology1_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(terminology2, path) as CatalogueItem

        then:
        catalogueItem == null
    }

    void "test getting code set by ID and path"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        CodeSet 1 by ID
         */
        when:
        Path path = Path.from('cs:')
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet1.id

        /*
        CodeSet 2 by ID
        */
        when:
        path = Path.from('cs:')
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet2.id

        /*
        CodeSet 1 by ID and path
         */
        when:
        path = Path.from(codeSet1)
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet1.id

        /*
        CodeSet 2 by ID and path
         */
        when:
        path = Path.from(codeSet2)
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet2.id

        /*
        Code Set 1 by ID and wrong path
        */
        when:
        path = Path.from(codeSet2)
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        !catalogueItem

        /*
        CodeSet 2 by ID and wrong path
         */
        when:
        path = Path.from(codeSet1)
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        !catalogueItem

        /*
        CodeSet 1 by path
        */
        when:
        path = Path.from(codeSet1)
        catalogueItem = pathService.findResourceByPathFromRootClass(CodeSet, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet1.id

        /*
        CodeSet 2 by path
        */
        when:
        path = Path.from(codeSet2)
        catalogueItem = pathService.findResourceByPathFromRootClass(CodeSet, path) as CatalogueItem

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id == codeSet2.id
    }

    void "test getting terms for codeset"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        CodeSet 1 Term 1
         */
        when:
        Path path = Path.from("cs:|tm:${terminology1_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology1_term1.id

        /*
        CodeSet 1 Term 2
        */
        when:
        path = Path.from("cs:|tm:${terminology1_term2.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        catalogueItem.label == terminology1_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology1_term2.id

        /*
        CodeSet 2 Term 1
        */
        when:
        path = Path.from("cs:|tm:${terminology2_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology2_term1.id

        /*
        CodeSet 2 Term 2
        */
        when:
        path = Path.from("cs:|tm:${terminology2_term2.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        catalogueItem.label == terminology2_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id == terminology2_term2.id

        /*
        Try to get a term which doesn't exist on CodeSet 1
        */
        when:
        path = Path.from("cs:|tm:${terminology2_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet1, path) as CatalogueItem

        then:
        catalogueItem == null

        /*
        Try to get a term which doesn't exist on CodeSet 2
        */
        when:
        path = Path.from("cs:|tm:${terminology1_term1.pathIdentifier}")
        catalogueItem = pathService.findResourceByPathFromRootResource(codeSet2, path) as CatalogueItem

        then:
        catalogueItem == null
    }

    void "test get Terminology by path when there is a branch"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        Terminology 3 by path. When using the label 'terminology 3' we expect to retrieve the terminology on the 
        main branch, rather than the one on the draft branch
        */
        when:
        Path path = Path.from('te:terminology 3')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 3'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology3main.id
        catalogueItem.description == terminology3main.description

        when: 'using the branch name main'
        path = Path.from('te:terminology 3:main')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 3'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology3main.id

        when: 'using the branch name draft'
        path = Path.from('te:terminology 3:draft')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 3'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology3draft.id

    }

    @PendingFeature
    void "test get Terminology by path when there are finalised and non-finalised versions"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        Terminology 4 by path. When using the label 'terminology 4' we expect to retrieve the
        non-finalised version. which will be the main branch
        */
        when:
        Path path = Path.from('te:terminology 4')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 4'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology4notFinalised.id
        catalogueItem.description == terminology4notFinalised.description

        when: 'using the version'
        path = Path.from('te:terminology 4:1.0.0')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 4'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology4finalised.id

    }

    @PendingFeature
    void "test get Terminology by path when there are two finalised versions"() {
        given:
        setupData()
        CatalogueItem catalogueItem

        /*
        Terminology 5 by path. When using the label 'terminology 5' we expect to retrieve the
        finalised version 2.0.0.
        */
        when:
        Path path = Path.from('te:terminology 5')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 5'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology5second.id
        catalogueItem.modelVersion.toString() == '2.0.0'

        when: 'using version 2.0.0'
        path = Path.from('te:terminology 5:2.0.0')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 5'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology5second.id
        catalogueItem.modelVersion.toString() == '2.0.0'

        when: 'using version 2'
        path = Path.from('te:terminology 5:2')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 5'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology5second.id
        catalogueItem.modelVersion.toString() == '2.0.0'

        when: 'using version 1'
        path = Path.from('te:terminology 5:1')
        catalogueItem = pathService.findResourceByPathFromRootClass(Terminology, path) as CatalogueItem

        then:
        catalogueItem.label == 'terminology 5'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id == terminology5second.id
        catalogueItem.modelVersion.toString() == '2.0.0'
    }

}
