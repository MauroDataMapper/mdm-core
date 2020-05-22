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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class BreadcrumbTreeSpec extends BaseUnitSpec implements DomainUnitTest<BreadcrumbTree> {

    BasicModel basicModel
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel, BasicModelItem)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        basicModel = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc)
        checkAndSave(misc)
        checkAndSave(basicModel)
    }

    void 'test a simple tree'() {

        when:
        check(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.errors.errorCount == 5

        when:
        domain.domainId = basicModel.id
        domain.label = basicModel.label
        domain.domainType = basicModel.domainType
        domain.finalised = basicModel.finalised
        domain.topBreadcrumbTree = true
        check(domain)

        then:
        !domain.errors.hasErrors()

        and:
        domain.breadcrumb.id == basicModel.id
        domain.breadcrumb.label == 'test'
        domain.breadcrumb.domainType == BasicModel.simpleName
        !domain.breadcrumb.finalised
        domain.tree == "${basicModel.id}|${BasicModel.simpleName}|test|false"
    }

    void "test Modelitem breadcrumb"() {
        when:
        BasicModelItem con = new BasicModelItem(createdBy: editor.emailAddress, label: 'content')
        basicModel.addToModelItems(con)
        checkAndSave(basicModel)

        then:
        BreadcrumbTree.count() == 2

        then:
        con.breadcrumbTree
        con.breadcrumbTree.domainId == con.id
        con.breadcrumbTree.label == 'content'
        con.breadcrumbTree.domainType == BasicModelItem.simpleName
        con.breadcrumbTree.parent
        con.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${con.id}|${BasicModelItem.simpleName}|content|null"""

        when:
        con.addToChildModelItems(createdBy: editor.emailAddress, label: 'child')
        checkAndSave(con)

        then:
        BreadcrumbTree.count() == 3

        and:
        con.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${con.id}|${BasicModelItem.simpleName}|content|null"""

        when:
        BasicModelItem child = BasicModelItem.findByLabel('child')

        then:
        child

        and:
        child.breadcrumbTree
        child.breadcrumbTree.domainId == child.id
        child.breadcrumbTree.label == 'child'
        child.breadcrumbTree.domainType == BasicModelItem.simpleName
        child.breadcrumbTree.parent
        child.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${con.id}|${BasicModelItem.simpleName}|content|null
${child.id}|${BasicModelItem.simpleName}|child|null"""
    }

    void "test moving Modelitem breadcrumb"() {
        when:
        BasicModelItem con = new BasicModelItem(createdBy: editor.emailAddress, label: 'content')
        basicModel.addToModelItems(con)
        con.addToChildModelItems(createdBy: editor.emailAddress, label: 'child')
        checkAndSave(basicModel)

        then:
        BreadcrumbTree.count() == 3

        and:
        con.breadcrumbTree
        con.breadcrumbTree.domainId == con.id
        con.breadcrumbTree.label == 'content'
        con.breadcrumbTree.domainType == BasicModelItem.simpleName
        con.breadcrumbTree.parent
        con.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${con.id}|${BasicModelItem.simpleName}|content|null"""

        when:
        BasicModelItem child = BasicModelItem.findByLabel('child')

        then:
        child

        and:
        child.breadcrumbTree
        child.breadcrumbTree.domainId == child.id
        child.breadcrumbTree.label == 'child'
        child.breadcrumbTree.domainType == BasicModelItem.simpleName
        child.breadcrumbTree.parent
        child.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${con.id}|${BasicModelItem.simpleName}|content|null
${child.id}|${BasicModelItem.simpleName}|child|null"""

        when:
        con.removeFromChildModelItems(child)
        basicModel.addToModelItems(child)
        checkAndSave(child)

        then:
        BreadcrumbTree.count() == 3
        child.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${child.id}|${BasicModelItem.simpleName}|child|null"""
    }

    void "test 2 Modelitems in tree breadcrumb"() {
        when:
        BasicModelItem bmi1 = new BasicModelItem(createdBy: editor.emailAddress, label: 'bmi1')
        BasicModelItem bmi2 = new BasicModelItem(createdBy: editor.emailAddress, label: 'bmi2')
        basicModel.addToModelItems(bmi1)
        basicModel.addToModelItems(bmi2)
        checkAndSave(basicModel)

        then:
        BreadcrumbTree.count() == 3

        then:
        bmi1.breadcrumbTree
        bmi1.breadcrumbTree.domainId == bmi1.id
        bmi1.breadcrumbTree.label == 'bmi1'
        bmi1.breadcrumbTree.domainType == BasicModelItem.simpleName
        bmi1.breadcrumbTree.parent
        bmi1.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${bmi1.id}|${BasicModelItem.simpleName}|bmi1|null"""

        and:
        bmi2.breadcrumbTree
        bmi2.breadcrumbTree.domainId == bmi2.id
        bmi2.breadcrumbTree.label == 'bmi2'
        bmi2.breadcrumbTree.domainType == BasicModelItem.simpleName
        bmi2.breadcrumbTree.parent
        bmi2.breadcrumbTree.tree == """${basicModel.id}|${BasicModel.simpleName}|test|false
${bmi2.id}|${BasicModelItem.simpleName}|bmi2|null"""

    }
}
