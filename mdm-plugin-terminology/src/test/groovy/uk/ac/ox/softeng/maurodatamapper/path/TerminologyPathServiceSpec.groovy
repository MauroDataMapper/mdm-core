/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.path

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.terminology.TerminologyService
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSetService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Stepwise

import java.time.OffsetDateTime
import java.time.ZoneOffset

@Slf4j
@Stepwise
class TerminologyPathServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<PathService> {
    
    UserSecurityPolicyManager userSecurityPolicyManager

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
    def setup() {
        log.debug('Setting up TerminologyPathServiceSpec Unit')

        mockArtefact(TermService)
        mockArtefact(TerminologyService)
        mockArtefact(CodeSetService)
        mockDomains(Terminology, CodeSet, Term)

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

        terminology3draft = new Terminology(createdByUser: admin, label: 'terminology 3', description: 'terminology 3 on draft', folder: testFolder, authority: testAuthority, branchName: 'draft')
        checkAndSave(terminology3draft)

        terminology4finalised = new Terminology(createdByUser: admin, label: 'terminology 4', description: 'terminology 4 finalised', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology4finalised)
        terminology4finalised.finalised = true
        terminology4finalised.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology4finalised.breadcrumbTree.finalise()
        terminology4finalised.modelVersion = Version.from('1.0.0')
        checkAndSave(terminology4finalised)

        terminology4notFinalised = new Terminology(createdByUser: admin, label: 'terminology 4', description: 'terminology 4 not finalised', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology4notFinalised)    

        terminology5first = new Terminology(createdByUser: admin, label: 'terminology 5', description: 'terminology 5 1.0.0', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology5first)
        terminology5first.finalised = true
        terminology5first.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology5first.breadcrumbTree.finalise()
        terminology5first.modelVersion = Version.from('1.0.0')
        checkAndSave(terminology5first)

        terminology5second = new Terminology(createdByUser: admin, label: 'terminology 5', description: 'terminology 5 2.0.0', folder: testFolder, authority: testAuthority)
        checkAndSave(terminology5second)
        terminology5second.finalised = true
        terminology5second.dateFinalised = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)
        terminology5second.breadcrumbTree.finalise()
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

    void "test truth"() {
        when:
        int a = 42

        then:
        a == 42
    }

    void "test getting terminology by ID and path"() {
        Map params
        CatalogueItem catalogueItem

        /*
        Terminology 1 by ID
         */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology1.id)

        /*
        Terminology 2 by ID
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology2.id)

        /*
        Terminology 1 by ID and path
         */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:${terminology1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology1.id)

        /*
        Terminology 2 by ID and path
         */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:${terminology2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology2.id)

        /*
         Terminology 1 by ID and wrong path
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:${terminology2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology1.id)

        /*
        Terminology 2 by ID and wrong path
         */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:${terminology1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology2.id)

        /*
        Terminology 1 by path
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'path': "te:${terminology1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology1.id)

        /*
        Terminology 2 by path
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'path': "te:${terminology2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2.label
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology2.id)
    }

    void "test getting terms for terminology"() {
        Map params
        CatalogueItem catalogueItem

        /*
        Terminology 1 Term 1
         */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:|tm:${terminology1_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology1_term1.id)

        /*
        Terminology 1 Term 2
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:|tm:${terminology1_term2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology1_term2.id)

        /*
        Terminology 2 Term 1
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:|tm:${terminology2_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology2_term1.id)

        /*
        Terminology 2 Term 2
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:|tm:${terminology2_term2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology2_term2.id)

        /*
        Try to get a term which doesn't exist on Terminology 1
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology1.id.toString(), 'path': "te:|tm:${terminology2_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem == null

        /*
        Try to get a term which doesn't exist on Terminology 2
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'catalogueItemId': terminology2.id.toString(), 'path': "te:|tm:${terminology1_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem == null
    }

    void "test getting code set by ID and path"() {
        Map params
        CatalogueItem catalogueItem

        /*
        CodeSet 1 by ID
         */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet1.id)

        /*
        CodeSet 2 by ID
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet2.id)

        /*
        CodeSet 1 by ID and path
         */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:${codeSet1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet1.id)

        /*
        CodeSet 2 by ID and path
         */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:${codeSet2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet2.id)

        /*
        Code Set 1 by ID and wrong path
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:${codeSet2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet1.id)

        /*
        CodeSet 2 by ID and wrong path
         */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:${codeSet1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet2.id)

        /*
        CodeSet 1 by path
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'path': "cs:${codeSet1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet1.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet1.id)

        /*
        CodeSet 2 by path
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'path': "cs:${codeSet2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == codeSet2.label
        catalogueItem.domainType == "CodeSet"
        catalogueItem.id.equals(codeSet2.id)
    }

    void "test getting terms for codeset"() {
        Map params
        CatalogueItem catalogueItem

        /*
        CodeSet 1 Term 1
         */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:|tm:${terminology1_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology1_term1.id)

        /*
        CodeSet 1 Term 2
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:|tm:${terminology1_term2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology1_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology1_term2.id)

        /*
        CodeSet 2 Term 1
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:|tm:${terminology2_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2_term1.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology2_term1.id)

        /*
        CodeSet 2 Term 2
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:|tm:${terminology2_term2.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == terminology2_term2.label
        catalogueItem.domainType == "Term"
        catalogueItem.id.equals(terminology2_term2.id)

        /*
        Try to get a term which doesn't exist on CodeSet 1
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet1.id.toString(), 'path': "cs:|tm:${terminology2_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem == null

        /*
        Try to get a term which doesn't exist on CodeSet 2
        */
        when:
        params = ['catalogueItemDomainType': 'codeSets', 'catalogueItemId': codeSet2.id.toString(), 'path': "cs:|tm:${terminology1_term1.label}"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem == null
    }

    void "test get Terminology by path when there is a branch"() {
        Map params
        CatalogueItem catalogueItem
        
        /*
        Terminology 3 by path. When using the label 'terminology 3' we expect to retrieve the terminology on the 
        main branch, rather than the one on the draft branch
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'path': "te:terminology 3"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == 'terminology 3'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology3main.id)
        catalogueItem.description.equals(terminology3main.description)

    }

    void "test get Terminology by path when there are finalised and non-finalised versions"() {
        Map params
        CatalogueItem catalogueItem
        
        /*
        Terminology 4 by path. When using the label 'terminology 4' we expect to retrieve the
        non-finalised version.
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'path': "te:terminology 4"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == 'terminology 4'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology4notFinalised.id)
        catalogueItem.description.equals(terminology4notFinalised.description)

    }    

    void "test get Terminology by path when there are two finalised versions"() {
        Map params
        CatalogueItem catalogueItem
        
        /*
        Terminology 5 by path. When using the label 'terminology 5' we expect to retrieve the
        finalised version 2.0.0.
        */
        when:
        params = ['catalogueItemDomainType': 'terminologies', 'path': "te:terminology 5"]
        catalogueItem = service.findCatalogueItemByPath(PublicAccessSecurityPolicyManager.instance, params)

        then:
        catalogueItem.label == 'terminology 5'
        catalogueItem.domainType == "Terminology"
        catalogueItem.id.equals(terminology5second.id)
        catalogueItem.description.equals(terminology5second.description)
        catalogueItem.modelVersion.toString() == '2.0.0'

    }      

}
