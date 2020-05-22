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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class ClassifierServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ClassifierService> {

    Classifier parent

    UUID id

    def setup() {
        mockDomains(Classifier, BasicModel)

        Classifier cl = new Classifier(createdBy: admin.emailAddress, label: 'classifier1')
        checkAndSave(cl)
        parent = new Classifier(createdBy: editor.emailAddress, label: 'parent classifier', description: 'the parent')
        parent.addToChildClassifiers(createdBy: reader1.emailAddress, label: 'reader classifier')
        checkAndSave(parent)
        Classifier nested = new Classifier(createdBy: reader1.emailAddress, label: 'nestedparent')
        checkAndSave(nested)
        nested.addToChildClassifiers(new Classifier(createdBy: editor.emailAddress, label: 'editor classifier'))
        checkAndSave(nested)
        parent.addToChildClassifiers(nested)
        checkAndSave(parent)

        id = nested.id
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<Classifier> classifierList = service.list(max: 2, offset: 3, sort: 'dateCreated')

        then:
        classifierList.size() == 2

        and:
        classifierList[0].label == 'nestedparent'
        classifierList[0].depth == 1
        classifierList[0].path == "/${parent.id}"

        and:
        classifierList[0].childClassifiers.size() == 1
        classifierList[0].childClassifiers[0].depth == 2
        classifierList[0].childClassifiers[0].path == "/${parent.id}/${id}"


        and:
        classifierList[1].label == 'editor classifier'
        classifierList[1].depth == 2
    }

    void "test count"() {
        expect:
        service.count() == 5
    }

    void "test delete"() {
        expect:
        service.count() == 5

        when:
        service.delete(id)

        then:
        service.count() == 3
    }

    void "test save"() {
        when:
        Classifier classifier = new Classifier(createdBy: reader1.emailAddress, label: 'another')
        parent.addToChildClassifiers(classifier)
        service.save(classifier)

        then:
        classifier.id != null

        when:
        Classifier classifier1 = service.get(classifier.id)

        then:
        classifier1

        when:
        classifier1 = service.get(parent.id)

        then:
        classifier1.childClassifiers.size() == 3
    }

    void 'test findOrCreateByLabel'() {

        when:
        Classifier classifier = service.findOrCreateByLabel('nestedparent')

        then:
        classifier.id == id

        when:
        classifier = service.findOrCreateByLabel('unknownclassifier')

        then:
        classifier
        !classifier.id
        classifier.label == 'unknownclassifier'
        !classifier.validate()
    }

    void 'test edit details'() {
        when:
        Classifier ci = service.get(id)
        def result = service.editInformation(ci, "new label", "new description")
        checkAndSave(ci)

        then:
        !result.hasErrors()

        when:
        Classifier ci2 = service.get(ci.getId())

        then:
        ci2.getLabel() == "new label"
        ci2.getDescription() == "new description"

    }

    void 'test findAllByUser'() {
        given:
        UserSecurityPolicyManager testPolicy

        when: 'using admin policy which can see all classifiers'
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(_) >> Classifier.list().collect {it.id}
        List<Classifier> list = service.findAllByUser(testPolicy)

        then:
        list.size() == 5

        when: 'using policy that can only read the id folder'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [id]
        list = service.findAllByUser(testPolicy)

        then:
        list.size() == 1

        when: 'using policy that provides an unknown id'
        testPolicy = Mock()
        testPolicy.listReadableSecuredResourceIds(_) >> [UUID.randomUUID()]
        list = service.findAllByUser(testPolicy)

        then:
        list.size() == 0

        when: 'using no access policy'
        list = service.findAllByUser(NoAccessSecurityPolicyManager.instance)

        then:
        list.size() == 0

        when: 'using public access policy'
        list = service.findAllByUser(PublicAccessSecurityPolicyManager.instance)

        then:
        list.size() == 5
    }

    void 'test findAllByCatalogueItemId with access to all classifiers'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Classifier) >> Classifier.list().collect {it.id}

        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        checkAndSave(basicModel)
        checkAndSave(basicModel2)

        basicModel.addToClassifiers(Classifier.findByLabel('classifier1'))
        basicModel.addToClassifiers(Classifier.findByLabel('parent classifier'))

        ModelService basicModelService = Stub() {
            getModelClass() >> BasicModel
            findByIdJoinClassifiers(_) >> {UUID id -> BasicModel.findByIdJoinClassifiers(id)}
        }
        service.catalogueItemServices = [basicModelService]

        when: 'searching for a non-existent id'
        List<Classifier> list = service.findAllByCatalogueItemId(testPolicy, UUID.randomUUID())

        then:
        list.isEmpty()

        when: 'searching for a catalogueitem with no classifiers'
        list = service.findAllByCatalogueItemId(testPolicy, basicModel2.id)

        then:
        list.isEmpty()

        when: 'searching for a catalogue item with classifiers'
        list = service.findAllByCatalogueItemId(testPolicy, basicModel.id)

        then:
        list.size() == 2

        and:
        list[0].label == 'classifier1'
        list[1].label == 'parent classifier'
    }

    void 'test findAllByCatalogueItemId with access to 1 classifier'() {
        given:
        UserSecurityPolicyManager testPolicy
        testPolicy = Mock()
        testPolicy.getUser() >> admin
        testPolicy.listReadableSecuredResourceIds(Classifier) >> [Classifier.findByLabel('classifier1').id]

        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        BasicModel basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        checkAndSave(basicModel)
        checkAndSave(basicModel2)

        basicModel.addToClassifiers(Classifier.findByLabel('classifier1'))
        basicModel.addToClassifiers(Classifier.findByLabel('parent classifier'))

        ModelService basicModelService = Stub() {
            getModelClass() >> BasicModel
            findByIdJoinClassifiers(_) >> {UUID id -> BasicModel.findByIdJoinClassifiers(id)}
        }
        service.catalogueItemServices = [basicModelService]

        when: 'searching for a non-existent id'
        List<Classifier> list = service.findAllByCatalogueItemId(testPolicy, UUID.randomUUID())

        then:
        list.isEmpty()

        when: 'searching for a catalogueitem with no classifiers'
        list = service.findAllByCatalogueItemId(testPolicy, basicModel2.id)

        then:
        list.isEmpty()

        when: 'searching for a catalogue item with classifiers'
        list = service.findAllByCatalogueItemId(testPolicy, basicModel.id)

        then:
        list.size() == 1

        and:
        list[0].label == 'classifier1'
    }
}
