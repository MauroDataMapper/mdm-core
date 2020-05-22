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
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.BasicEmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.CatalogueItemAwareServiceSpec
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

import grails.testing.services.ServiceUnitTest

class MetadataServiceSpec extends CatalogueItemAwareServiceSpec<Metadata, MetadataService> implements ServiceUnitTest<MetadataService> {

    MauroDataMapperService mauroDataMapperService
    UUID id

    def setup() {
        mockDomains(Folder, BasicModel, Edit, Metadata)
        mockArtefact(BasicEmailProviderService)
        mockArtefact(MauroDataMapperServiceProviderService)

        mauroDataMapperService = applicationContext.getBean(BasicEmailProviderService)
        MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService =
            applicationContext.getBean(MauroDataMapperServiceProviderService)


        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))

        Metadata metadata = new Metadata(createdBy: reader1.emailAddress, namespace: 'https://integration.test.com', key: 'key1', value: 'value1')
        basicModel.addToMetadata(metadata)
        basicModel.addToMetadata(createdBy: editor.emailAddress, namespace: 'https://integration.test.com', key: 'key2', value: 'value2')
        // ensure unique between item and key is tested
        basicModel.addToMetadata(createdBy: pending.emailAddress, namespace: 'https://integration.test.com/test', key: 'key1', value: 'value1')
        basicModel.addToMetadata(createdBy: reader1.emailAddress, namespace: 'https://integration.test.com', key: 'key4', value: 'value4')
        basicModel.addToMetadata(createdBy: reader2.emailAddress, namespace: 'https://integration.test.com', key: 'key5', value: 'value5')
        basicModel.
            addToMetadata(createdBy: admin.emailAddress, namespace: mauroDataMapperService.namespace, key: 'email.subject', value: 'test subject')

        checkAndSave(basicModel)
        id = metadata.id

        ModelService basicModelService = Stub() {
            get(_) >> basicModel
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeMetadataFromCatalogueItem(basicModel.id, metadata) >> {
                basicModel.removeFromMetadata(metadata)
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
        List<Metadata> metadataList = service.list(max: 2, offset: 2)

        then:
        metadataList.size() == 2

        and:
        metadataList[0].namespace == 'https://integration.test.com/test'
        metadataList[0].key == 'key1'
        metadataList[0].value == 'value1'

        and:
        metadataList[1].namespace == 'https://integration.test.com'
        metadataList[1].key == 'key4'
        metadataList[1].value == 'value4'
    }

    void "test count"() {
        expect:
        service.count() == 6
    }

    void "test delete"() {
        expect:
        service.count() == 6

        when:
        service.delete(id)

        then:
        service.count() == 5
    }

    void 'test finding all namespaces'() {

        when:
        Collection<String> namespaces = Metadata.findAllDistinctNamespaces()

        then:
        namespaces.size() == 3
        namespaces.find {it == 'https://integration.test.com'}
        namespaces.find {it == 'https://integration.test.com/test'}
        namespaces.find {it == mauroDataMapperService.namespace}
    }

    void 'test findAllKeysByNamespace'() {
        when:
        Collection<String> keys = Metadata.findAllDistinctKeysByNamespace('https://integration.test.com')

        then:
        keys.size() == 4
        keys.find {it == 'key1'}
        keys.find {it == 'key2'}
        keys.find {it == 'key4'}
        keys.find {it == 'key5'}
    }

    void 'test find namespace keys by plugin or namespace'() {

        when: 'non-existent namespace or plugin'
        def result = service.findNamespaceKeysByServiceOrNamespace(null, 'https://integration.test.com/none')

        then:
        result.namespace == 'https://integration.test.com/none'
        result.editable
        result.keys.size() == 0

        when: 'only namespace specified'
        result = service.findNamespaceKeysByServiceOrNamespace(null, 'https://integration.test.com')

        then:
        result.namespace == 'https://integration.test.com'
        result.editable
        result.keys.size() == 4
        result.keys.find {it == 'key1'}
        result.keys.find {it == 'key2'}
        result.keys.find {it == 'key4'}
        result.keys.find {it == 'key5'}

        when: 'namespace and plugin with no specified keys'
        result = service.findNamespaceKeysByServiceOrNamespace(mauroDataMapperService, mauroDataMapperService.namespace)

        then:
        result.namespace == mauroDataMapperService.namespace
        result.editable
        result.keys.size() == 1
        result.keys.find {it == 'email.subject'}
    }

    void 'test find all namespace keys'() {

        when:
        def result = service.findNamespaceKeys()

        then:
        result.size() == 3

        when:
        def test = result.find {it.namespace == 'https://integration.test.com'}
        def testTest = result.find {it.namespace == 'https://integration.test.com/test'}
        def plugin = result.find {it.namespace == mauroDataMapperService.namespace}

        then:
        test.editable
        test.keys.size() == 4
        test.keys.find {it == 'key1'}
        test.keys.find {it == 'key2'}
        test.keys.find {it == 'key4'}
        test.keys.find {it == 'key5'}

        and:
        plugin.editable
        plugin.keys.size() == 1
        plugin.keys.find {it == 'email.subject'}

        and:
        testTest.editable
        testTest.keys.size() == 1
        testTest.keys.find {it == 'key1'}
    }

    void 'test find namespacekeys for namespace'() {

        when:
        def result = service.findNamespaceKeysIlikeNamespace('https://integration.test.com')

        then:
        result

        when:
        def nk = result.first()

        then:
        nk.namespace == 'https://integration.test.com'
        nk.editable
        nk.keys.size() == 4
        nk.keys.find {it == 'key1'}
        nk.keys.find {it == 'key2'}
        nk.keys.find {it == 'key4'}
        nk.keys.find {it == 'key5'}

        when:
        result = service.findNamespaceKeysIlikeNamespace(mauroDataMapperService.namespace)

        then:
        result

        when:
        nk = result.first()

        then:
        nk.namespace == mauroDataMapperService.namespace
        nk.editable
        nk.keys.size() == 1
        nk.keys.find {it == 'email.subject'}
    }

    void 'test findByCatalogueItemIdAndId for root metadata'() {
        when:
        Metadata metadata = service.findByCatalogueItemIdAndId(UUID.randomUUID(), id)

        then:
        !metadata

        when:
        metadata = service.findByCatalogueItemIdAndId(basicModel.id, id)

        then:
        metadata
        metadata.id == id
        metadata.value == 'value1'
    }

    @Override
    Metadata getAwareItem() {
        Metadata.get(id)
    }

    @Override
    Metadata getUpdatedAwareItem() {
        Metadata md = Metadata.get(id)
        md.value = 'altered'
        md
    }

    @Override
    int getExpectedCountOfAwareItemsInBasicModel() {
        6
    }

    @Override
    String getChangedPropertyName() {
        'value'
    }
}
