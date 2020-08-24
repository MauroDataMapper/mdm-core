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

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.CatalogueItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class VersionLinkServiceSpec extends CatalogueItemAwareServiceSpec<VersionLink, VersionLinkService>
    implements ServiceUnitTest<VersionLinkService> {

    UUID id
    BasicModel basicModel2
    BasicModel basicModel3
    Authority testAuthority

    def setup() {
        mockDomains(Folder, BasicModel, Edit, VersionLink, Authority)
        testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                     authority: testAuthority)
        basicModel3 = new BasicModel(label: 'dm3', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                     authority: testAuthority)
        checkAndSave basicModel
        checkAndSave(basicModel2)
        checkAndSave(basicModel3)

        VersionLink sl1 = new VersionLink(createdBy: admin.emailAddress, linkType: VersionLinkType.SUPERSEDED_BY_FORK)
        basicModel.addToVersionLinks(sl1)
        sl1.setTargetModel(basicModel2)
        VersionLink sl2 = new VersionLink(createdBy: admin.emailAddress, linkType: VersionLinkType.NEW_FORK_OF)
        basicModel.addToVersionLinks(sl2)
        sl2.setTargetModel(basicModel3)

        checkAndSave(basicModel)

        id = sl1.id

        ModelService basicModelService = Stub() {
            getAll(_) >> {List<UUID> ids -> BasicModel.getAll(ids)}
            get(_) >> {UUID id -> BasicModel.get(id)}
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeVersionLinkFromModel(_, _) >> {UUID id, VersionLink versionLink ->
                BasicModel bm = BasicModel.get(id)
                bm.removeFromVersionLinks(versionLink)
            }
        }
        service.catalogueItemServices = [basicModelService]
        service.modelServices = [basicModelService]

    }


    @Override
    VersionLink getAwareItem() {
        VersionLink.get(id)
    }

    @Override
    VersionLink getUpdatedAwareItem() {
        VersionLink sl = VersionLink.get(id)
        sl.linkType = VersionLinkType.NEW_FORK_OF
        sl
    }

    @Override
    int getExpectedCountOfAwareItemsInBasicModel() {
        2
    }

    @Override
    String getChangedPropertyName() {
        'linkType'
    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<VersionLink> versionLinkList = service.list(max: 2, offset: 1)

        then:
        versionLinkList.size() == 1

        and:
        versionLinkList[0].linkType == VersionLinkType.NEW_FORK_OF
        versionLinkList[0].modelId == BasicModel.findByLabel('dm1').id
        versionLinkList[0].targetModelId == BasicModel.findByLabel('dm3').id
    }

    void "test count"() {
        expect:
        service.count() == 2
    }

    void "test delete"() {
        expect:
        service.count() == 2

        when: 'deleting should delete'
        service.delete(id)

        then:
        service.count() == 1
    }

    void 'test findAllBySourceModelId'() {
        when:
        List<VersionLink> links = service.findAllBySourceModelId(BasicModel.findByLabel('dm1').id)

        then:
        !links.isEmpty()
        links.size() == 2

        when:
        links = service.findAllBySourceModelId(BasicModel.findByLabel('dm2').id)

        then:
        links.isEmpty()

        when:
        links = service.findAllBySourceModelId(BasicModel.findByLabel('dm3').id)

        then:
        links.isEmpty()
    }

    void 'test findAllByTargetModelId'() {
        when:
        List<VersionLink> links = service.findAllByTargetModelId(BasicModel.findByLabel('dm1').id)

        then:
        links.isEmpty()

        when:
        links = service.findAllByTargetModelId(BasicModel.findByLabel('dm2').id)

        then:
        !links.isEmpty()
        links.size() == 1

        when:
        links = service.findAllByTargetModelId(BasicModel.findByLabel('dm3').id)

        then:
        !links.isEmpty()
        links.size() == 1
    }

    void 'test findAllByAnyModelId'() {
        when:
        List<VersionLink> links = service.findAllBySourceOrTargetModelId(BasicModel.findByLabel('dm1').id)

        then:
        !links.isEmpty()
        links.size() == 2

        when:
        links = service.findAllBySourceOrTargetModelId(BasicModel.findByLabel('dm2').id)

        then:
        !links.isEmpty()
        links.size() == 1

        when:
        links = service.findAllBySourceOrTargetModelId(BasicModel.findByLabel('dm3').id)

        then:
        !links.isEmpty()
        links.size() == 1
    }

    void 'test findBySourceModelAndTargetModelAndLinkType'() {
        when:
        VersionLink sl = service.findBySourceModelAndTargetModelAndLinkType(BasicModel.findByLabel('dm1'),
                                                                            BasicModel.findByLabel('dm2'),
                                                                            VersionLinkType.NEW_FORK_OF)

        then:
        !sl

        when:
        sl = service.findBySourceModelAndTargetModelAndLinkType(BasicModel.findByLabel('dm1'),
                                                                BasicModel.findByLabel('dm2'),
                                                                VersionLinkType.SUPERSEDED_BY_FORK)

        then:
        sl
    }

    void 'test deleteBySourceAndTargetAndLinkType'() {
        when:
        service.deleteBySourceModelAndTargetModelAndLinkType(BasicModel.findByLabel('dm1'),
                                                             BasicModel.findByLabel('dm2'),
                                                             VersionLinkType.NEW_FORK_OF)

        then:
        service.count() == 2

        when:
        service.deleteBySourceModelAndTargetModelAndLinkType(BasicModel.findByLabel('dm1'),
                                                             BasicModel.findByLabel('dm2'),
                                                             VersionLinkType.SUPERSEDED_BY_FORK)

        then:
        service.count() == 1
    }

    void 'test create semantic link'() {
        when:
        VersionLink sl = service.createVersionLink(editor, BasicModel.findByLabel('dm2'),
                                                   BasicModel.findByLabel('dm3'),
                                                   VersionLinkType.NEW_FORK_OF)

        then:
        sl

        when:
        VersionLink sls = service.save(sl)

        then:
        sls

        when:
        VersionLink slf = service.get(sls.id)

        then:
        slf
    }

    void 'test loadCatalogueItemsIntoSemanticLink'() {
        when:
        VersionLink sl = service.get(id)

        then:
        !sl.catalogueItem
        !sl.targetModel

        when:
        sl = service.loadModelsIntoVersionLink(sl)

        then:
        sl.catalogueItem
        sl.targetModel

        and:
        sl.catalogueItem.label == 'dm1'
        sl.targetModel.label == 'dm2'

    }

    void 'test loadCatalogueItemsIntoVersionLinks'() {
        when:
        List<VersionLink> sls = service.list()

        then:
        sls.every {!it.catalogueItem}
        sls.every {!it.targetModel}

        when:
        sls = service.loadModelsIntoVersionLinks(sls)

        then:
        sls.every {it.catalogueItem}
        sls.every {it.targetModel}
    }

    void 'test filtering of document superseded models'() {
        given:
        BasicModel basicModel4 = new BasicModel(label: 'dm4', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel4

        // bm1 SUPERSEDED_BY_FORK bm2
        // bm1 NEW_FORK_OF bm3
        when:
        List<UUID> ids = service.filterModelIdsWhereModelIdIsDocumentSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 0

        // bm3 NEW_DOCUMENTATION_VERSION_OF bm2 -- bm2 is superseded
        // bm4 SUPERSEDED_BY_DOCUMENTATION bm3 -- bm4 is superseded
        when:
        basicModel3.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
                                                      targetModel: BasicModel.findByLabel('dm2')))
        basicModel4.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.SUPERSEDED_BY_DOCUMENTATION,
                                                      targetModel: BasicModel.findByLabel('dm3')))
        checkAndSave(basicModel3)
        checkAndSave(basicModel4)
        ids = service.filterModelIdsWhereModelIdIsDocumentSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 2
        ids.any {it == basicModel2.id}
        ids.any {it == basicModel4.id}

    }

    void 'test filtering of model superseded models'() {
        given:
        BasicModel basicModel4 = new BasicModel(label: 'dm4', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel4

        // bm1 SUPERSEDED_BY_FORK bm2 -- bm1 superseded
        // bm1 NEW_FORK_OF bm3 -- bm3 supersed
        when:
        List<UUID> ids = service.filterModelIdsWhereModelIdIsModelSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 2
        ids.any { it == basicModel.id }
        ids.any { it == basicModel3.id }

        // bm3 NEW_FORK_OF bm2 -- bm2 is superseded
        // bm4 SUPERSEDED_BY_FORK bm3 -- bm4 is superseded
        when:
        basicModel3.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.NEW_FORK_OF,
                                                      targetModel: BasicModel.findByLabel('dm2')))
        basicModel4.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.SUPERSEDED_BY_FORK,
                                                      targetModel: BasicModel.findByLabel('dm3')))
        checkAndSave(basicModel3)
        checkAndSave(basicModel4)
        ids = service.filterModelIdsWhereModelIdIsModelSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 4
        ids.any {it == basicModel.id}
        ids.any {it == basicModel3.id}
        ids.any {it == basicModel2.id}
        ids.any {it == basicModel4.id}

        // bm3 NEW_DOCUMENTATION_VERSION_OF bm2 -- not a relevant supersede
        // bm4 SUPERSEDED_BY_DOCUMENTATION bm3 -- not a relevant supersede
        when:
        basicModel3.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
                                                      targetModel: BasicModel.findByLabel('dm2')))
        basicModel4.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.SUPERSEDED_BY_DOCUMENTATION,
                                                      targetModel: BasicModel.findByLabel('dm3')))
        checkAndSave(basicModel3)
        checkAndSave(basicModel4)
        ids = service.filterModelIdsWhereModelIdIsModelSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 4
        ids.any {it == basicModel.id}
        ids.any {it == basicModel3.id}
        ids.any {it == basicModel2.id}
        ids.any {it == basicModel4.id}
    }

    void 'test filtering of superseded models'() {
        given:
        BasicModel basicModel4 = new BasicModel(label: 'dm4', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel4

        // bm1 SUPERSEDED_BY_FORK bm2 -- bm1 superseded
        // bm1 NEW_FORK_OF bm3 -- bm3 superseded
        when:
        List<UUID> ids = service.filterModelIdsWhereModelIdIsSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 2
        ids.any { it == basicModel.id }
        ids.any { it == basicModel3.id }

        // bm3 NEW_DOCUMENTATION_VERSION_OF bm2 -- bm2 is superseded
        // bm4 SUPERSEDED_BY_DOCUMENTATION bm3 -- bm4 is superseded
        when:
        basicModel3.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
                                                      targetModel: BasicModel.findByLabel('dm2')))
        basicModel4.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.SUPERSEDED_BY_DOCUMENTATION,
                                                      targetModel: BasicModel.findByLabel('dm3')))
        checkAndSave(basicModel3)
        checkAndSave(basicModel4)
        ids = service.filterModelIdsWhereModelIdIsSuperseded('BasicModel', BasicModel.list().id)

        then:
        ids.size() == 4
        ids.any {it == basicModel.id}
        ids.any {it == basicModel3.id}
        ids.any {it == basicModel2.id}
        ids.any {it == basicModel4.id}

    }

    void 'test finding latest superseding model'() {
        given:
        BasicModel basicModel4 = new BasicModel(label: 'dm4', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel4

        // bm1 SUPERSEDED_BY_FORK bm2 -- bm1 superseded
        // bm1 NEW_FORK_OF bm3 -- bm3 superseded
        when:
        VersionLink link = service.findLatestLinkSupersedingModelId('BasicModel', basicModel.id)

        then:
        link
        link.linkType == VersionLinkType.SUPERSEDED_BY_FORK
        link.targetModelId == basicModel2.id
        link.catalogueItemId == basicModel.id

        when:
        link = service.findLatestLinkSupersedingModelId('BasicModel', basicModel3.id)

        then:
        link
        link.linkType == VersionLinkType.NEW_FORK_OF
        link.targetModelId == basicModel3.id
        link.catalogueItemId == basicModel.id

        when:
        link = service.findLatestLinkSupersedingModelId('BasicModel', basicModel2.id)

        then:
        !link

        when:
        basicModel4.addToVersionLinks(new VersionLink(createdByUser: admin,
                                                      linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF,
                                                      targetModel: BasicModel.findByLabel('dm3')))
        checkAndSave(basicModel4)
        link = service.findLatestLinkSupersedingModelId('BasicModel', basicModel3.id)

        then:
        link
        link.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF
        link.targetModelId == basicModel3.id
        link.catalogueItemId == basicModel4.id
    }
}
