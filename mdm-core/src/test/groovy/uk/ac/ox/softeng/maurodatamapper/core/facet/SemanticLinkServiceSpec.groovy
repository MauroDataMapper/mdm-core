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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.CatalogueItemAwareServiceSpec

import grails.testing.services.ServiceUnitTest

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class SemanticLinkServiceSpec extends CatalogueItemAwareServiceSpec<SemanticLink, SemanticLinkService>
    implements ServiceUnitTest<SemanticLinkService> {

    UUID id

    def setup() {
        mockDomains(Folder, BasicModel, Edit, SemanticLink, Authority)

        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        BasicModel basicModel3 = new BasicModel(label: 'dm3', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel
        checkAndSave(basicModel2)
        checkAndSave(basicModel3)

        SemanticLink sl1 = new SemanticLink(createdBy: admin.emailAddress, linkType: SemanticLinkType.REFINES)
        basicModel.addToSemanticLinks(sl1)
        sl1.setTargetCatalogueItem(basicModel2)
        SemanticLink sl2 = new SemanticLink(createdBy: admin.emailAddress, linkType: SemanticLinkType.DOES_NOT_REFINE)
        basicModel.addToSemanticLinks(sl2)
        sl2.setTargetCatalogueItem(basicModel3)

        checkAndSave(basicModel)

        id = sl1.id

        ModelService basicModelService = Stub() {
            get(_) >> {UUID id -> BasicModel.get(id)}
            getAll(_) >> {List<UUID> ids -> BasicModel.getAll(ids)}
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeSemanticLinkFromCatalogueItem(_, _) >> {UUID id, SemanticLink semanticLink ->
                BasicModel bm = BasicModel.get(id)
                bm.semanticLinks.remove(semanticLink)
            }
        }
        service.catalogueItemServices = [basicModelService]

    }

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
        when:
        List<SemanticLink> semanticLinkList = service.list(max: 2, offset: 1)

        then:
        semanticLinkList.size() == 1

        and:
        semanticLinkList[0].linkType == SemanticLinkType.DOES_NOT_REFINE
        semanticLinkList[0].catalogueItemId == BasicModel.findByLabel('dm1').id
        semanticLinkList[0].targetCatalogueItemId == BasicModel.findByLabel('dm3').id
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

    void 'test findAllBySourceCatalogueItemId'() {
        when:
        List<SemanticLink> links = service.findAllBySourceCatalogueItemId(BasicModel.findByLabel('dm1').id)

        then:
        !links.isEmpty()
        links.size() == 2

        when:
        links = service.findAllBySourceCatalogueItemId(BasicModel.findByLabel('dm2').id)

        then:
        links.isEmpty()

        when:
        links = service.findAllBySourceCatalogueItemId(BasicModel.findByLabel('dm3').id)

        then:
        links.isEmpty()
    }

    void 'test findAllByTargetCatalogueItemId'() {
        when:
        List<SemanticLink> links = service.findAllByTargetCatalogueItemId(BasicModel.findByLabel('dm1').id)

        then:
        links.isEmpty()

        when:
        links = service.findAllByTargetCatalogueItemId(BasicModel.findByLabel('dm2').id)

        then:
        !links.isEmpty()
        links.size() == 1

        when:
        links = service.findAllByTargetCatalogueItemId(BasicModel.findByLabel('dm3').id)

        then:
        !links.isEmpty()
        links.size() == 1
    }

    void 'test findAllByAnyCatalogueItemId'() {
        when:
        List<SemanticLink> links = service.findAllBySourceOrTargetCatalogueItemId(BasicModel.findByLabel('dm1').id)

        then:
        !links.isEmpty()
        links.size() == 2

        when:
        links = service.findAllBySourceOrTargetCatalogueItemId(BasicModel.findByLabel('dm2').id)

        then:
        !links.isEmpty()
        links.size() == 1

        when:
        links = service.findAllBySourceOrTargetCatalogueItemId(BasicModel.findByLabel('dm3').id)

        then:
        !links.isEmpty()
        links.size() == 1
    }

    void 'test findBySourceCatalogueItemAndTargetCatalogueItemAndLinkType'() {
        when:
        SemanticLink sl = service.findBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(BasicModel.findByLabel('dm1'),
                                                                                             BasicModel.findByLabel('dm2'),
                                                                                             SemanticLinkType.ABSTRACTS)

        then:
        !sl

        when:
        sl = service.findBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(BasicModel.findByLabel('dm1'),
                                                                                BasicModel.findByLabel('dm2'),
                                                                                SemanticLinkType.REFINES)

        then:
        sl
    }

    void 'test deleteBySourceAndTargetAndLinkType'() {
        when:
        service.deleteBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(BasicModel.findByLabel('dm1'),
                                                                             BasicModel.findByLabel('dm2'),
                                                                             SemanticLinkType.ABSTRACTS)

        then:
        service.count() == 2

        when:
        service.deleteBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(BasicModel.findByLabel('dm1'),
                                                                             BasicModel.findByLabel('dm2'),
                                                                             SemanticLinkType.REFINES)

        then:
        service.count() == 1
    }

    void 'test create semantic link'() {
        when:
        SemanticLink sl = service.createSemanticLink(editor, BasicModel.findByLabel('dm2'),
                                                     BasicModel.findByLabel('dm3'),
                                                     SemanticLinkType.ABSTRACTS)

        then:
        sl

        when:
        SemanticLink sls = service.save(sl)

        then:
        sls

        when:
        SemanticLink slf = service.get(sls.id)

        then:
        slf
    }

    void 'test loadCatalogueItemsIntoSemanticLink'() {
        when:
        SemanticLink sl = service.get(id)

        then:
        !sl.catalogueItem
        !sl.targetCatalogueItem

        when:
        sl = service.loadCatalogueItemsIntoSemanticLink(sl)

        then:
        sl.catalogueItem
        sl.targetCatalogueItem

        and:
        sl.catalogueItem.label == 'dm1'
        sl.targetCatalogueItem.label == 'dm2'

    }

    void 'test loadCatalogueItemsIntoSemanticLinks'() {
        when:
        List<SemanticLink> sls = service.list()

        then:
        sls.every {!it.catalogueItem}
        sls.every {!it.targetCatalogueItem}

        when:
        sls = service.loadCatalogueItemsIntoSemanticLinks(sls)

        then:
        sls.every {it.catalogueItem}
        sls.every {it.targetCatalogueItem}
    }

    @Override
    SemanticLink getAwareItem() {
        SemanticLink.get(id)
    }

    @Override
    SemanticLink getUpdatedAwareItem() {
        SemanticLink sl = SemanticLink.get(id)
        sl.linkType = SemanticLinkType.DOES_NOT_REFINE
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
}
