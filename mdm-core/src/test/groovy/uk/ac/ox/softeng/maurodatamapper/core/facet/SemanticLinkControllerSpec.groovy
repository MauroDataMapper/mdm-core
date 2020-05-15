package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.ABSTRACTS
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.DOES_NOT_ABSTRACT
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.DOES_NOT_REFINE
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.REFINES

@Slf4j
class SemanticLinkControllerSpec extends ResourceControllerSpec<SemanticLink> implements
    DomainUnitTest<SemanticLink>,
    ControllerUnitTest<SemanticLinkController> {

    BasicModel basicModel
    BasicModel basicModel2
    BasicModel basicModel3

    def setup() {
        mockDomains(Folder, BasicModel)
        log.debug('Setting up semantic link controller unit')
        mockDomains(Folder, BasicModel, Edit, SemanticLink)

        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        basicModel3 = new BasicModel(label: 'dm3', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        checkAndSave basicModel
        checkAndSave(basicModel2)
        checkAndSave(basicModel3)

        BasicModelItem bmi = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress)
        basicModel.addToModelItems(bmi)

        SemanticLink sl1 = new SemanticLink(createdBy: admin.emailAddress, linkType: REFINES)
        basicModel.addToSemanticLinks(sl1)
        sl1.setTargetCatalogueItem(basicModel2)
        SemanticLink sl2 = new SemanticLink(createdBy: admin.emailAddress, linkType: DOES_NOT_REFINE)
        basicModel.addToSemanticLinks(sl2)
        sl2.setTargetCatalogueItem(basicModel3)

        checkAndSave(basicModel)

        domain.createdBy = admin.emailAddress
        domain.linkType = ABSTRACTS
        domain.setTargetCatalogueItem(bmi)
        basicModel3.addToSemanticLinks(domain)
        checkAndSave(domain)

        ModelService basicModelService = Stub() {
            get(_) >> {UUID id -> BasicModel.get(id)}
            getAll(_) >> {List<UUID> ids -> BasicModel.getAll(ids)}
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeSemanticLinkFromCatalogueItem(_, _) >> {UUID id, SemanticLink semanticLink ->
                BasicModel bm = BasicModel.get(id)
                bm.removeFromSemanticLinks(semanticLink)
            }
        }
        ModelItemService basicModelItemService = Stub() {
            get(_) >> {UUID id -> BasicModelItem.get(id)}
            getAll(_) >> {List<UUID> ids -> BasicModelItem.getAll(ids)}
            getModelClass() >> BasicModelItem
            handles('BasicModelItem') >> true
            removeSemanticLinkFromCatalogueItem(_, _) >> {UUID id, SemanticLink semanticLink ->
                BasicModelItem bm = BasicModelItem.get(id)
                bm.removeFromSemanticLinks(semanticLink)
            }
        }
        SemanticLinkService semanticLinkService = new SemanticLinkService()
        semanticLinkService.catalogueItemServices = [basicModelService, basicModelItemService]
        controller.semanticLinkService = semanticLinkService
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "targetCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm2"
      },
      "domainType": "SemanticLink",
      "sourceCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      },
      "linkType": "Refines",
      "id": "${json-unit.matches:id}"
    },
    {
      "targetCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
      },
      "domainType": "SemanticLink",
      "sourceCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      },
      "linkType": "Does Not Refine",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 3,
  "errors": [
    {
      "message": "Property [targetCatalogueItemId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    },
    {
      "message": "Property [targetCatalogueItemDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    },
    {
      "message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {
      "message": "Property [targetCatalogueItemId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    },
    {
      "message": "Property [targetCatalogueItemDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
    "domainType": "SemanticLink",
    "linkType": "Abstracts",
    "id": "${json-unit.matches:id}",
    "sourceCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
    },
    "targetCatalogueItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
    }
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "targetCatalogueItem": {
    "domainType": "BasicModelItem",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "bmi1",
    "breadcrumbs": [
      {
        "domainType": "BasicModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      }
    ]
  },
  "domainType": "SemanticLink",
  "sourceCatalogueItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Abstracts",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total":1, "errors": [{"message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.' +
        'SemanticLink] cannot be null"}]}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "targetCatalogueItem": {
    "domainType": "BasicModelItem",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "bmi1",
    "breadcrumbs": [
      {
        "domainType": "BasicModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      }
    ]
  },
  "domainType": "SemanticLink",
  "sourceCatalogueItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Does Not Abstract",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    SemanticLink invalidUpdate(SemanticLink instance) {
        instance.linkType = null
        instance
    }

    @Override
    SemanticLink validUpdate(SemanticLink instance) {
        instance.linkType = DOES_NOT_ABSTRACT
        instance
    }

    @Override
    SemanticLink getInvalidUnsavedInstance() {
        new SemanticLink(linkType: DOES_NOT_REFINE)
    }

    @Override
    SemanticLink getValidUnsavedInstance() {
        new SemanticLink(linkType: ABSTRACTS, targetCatalogueItem: basicModel3)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.catalogueItemDomainType = BasicModel.simpleName
        params.catalogueItemId = basicModel.id
    }

    @Override
    String getTemplate() {
        '''
    import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink

model {
    SemanticLink semanticLink
}

json {
    id semanticLink.id
    if(semanticLink.linkType) linkType semanticLink.linkType.label
    domainType semanticLink.domainType

    targetCatalogueItemId semanticLink.targetCatalogueItemId
    targetCatalogueItemDomainType semanticLink.targetCatalogueItemDomainType
}
    '''
    }
}