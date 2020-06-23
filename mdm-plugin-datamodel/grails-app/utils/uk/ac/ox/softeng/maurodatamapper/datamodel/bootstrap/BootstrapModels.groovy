package uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType

import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.DEVELOPMENT
import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

class BootstrapModels {

   public static final String COMPLEX_DATAMODEL_NAME = 'Complex Test DataModel'
    public static final String SIMPLE_DATAMODEL_NAME = 'Simple Test DataModel'


    static DataModel buildAndSaveSimpleDataModel(MessageSource messageSource, Folder folder) {
        DataModel simpleDataModel = new DataModel(createdBy: DEVELOPMENT, label: SIMPLE_DATAMODEL_NAME, folder: folder)

        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier simple',
                                                             readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)

        simpleDataModel.addToClassifiers(classifier)

        checkAndSave(messageSource, simpleDataModel)
        DataClass dataClass = new DataClass(createdBy: DEVELOPMENT, label: 'simple')

        simpleDataModel
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk2', value: 'mdv2')

            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk2', value: 'mdv2')

            .addToDataClasses(dataClass)

        checkAndSave(messageSource, simpleDataModel)

        dataClass.addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/simple', key: 'mdk1', value: 'mdv1')

        checkAndSave(messageSource, simpleDataModel)

        simpleDataModel
    }

    static DataModel buildAndSaveComplexDataModel(MessageSource messageSource, Folder folder) {
        DataModel dataModel = new DataModel(createdBy: DEVELOPMENT, label: COMPLEX_DATAMODEL_NAME, organisation: 'brc', author: 'admin person',
                                            folder: folder)
        checkAndSave(messageSource, dataModel)
        Classifier classifier = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier',
                                                             readableByAuthenticatedUsers: true)
        Classifier classifier1 = Classifier.findOrCreateWhere(createdBy: DEVELOPMENT, label: 'test classifier2',
                                                              readableByAuthenticatedUsers: true)
        checkAndSave(messageSource, classifier)
        checkAndSave(messageSource, classifier1)

        dataModel.addToClassifiers(classifier)
            .addToClassifiers(classifier1)

            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk1', value: 'mdv1')
            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com', key: 'mdk2', value: 'mdv2')

            .addToMetadata(createdBy: DEVELOPMENT, namespace: 'test.com/test', key: 'mdk1', value: 'mdv2')

            .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 1')

            .addToAnnotations(createdBy: DEVELOPMENT, label: 'test annotation 2', description: 'with description')

            .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'string'))

            .addToDataTypes(new PrimitiveType(createdBy: DEVELOPMENT, label: 'integer'))

            .addToDataClasses(createdBy: DEVELOPMENT, label: 'emptyclass', description: 'dataclass with desc')
            .addToDataTypes(new EnumerationType(createdBy: DEVELOPMENT, label: 'yesnounknown')
                                .addToEnumerationValues(key: 'Y', value: 'Yes')
                                .addToEnumerationValues(key: 'N', value: 'No')
                                .addToEnumerationValues(key: 'U', value: 'Unknown'))

        DataClass parent =
            new DataClass(createdBy: DEVELOPMENT, label: 'parent', minMultiplicity: 1, maxMultiplicity: -1, dataModel: dataModel)
        DataClass child = new DataClass(createdBy: DEVELOPMENT, label: 'child')
        parent.addToDataClasses(child)
        dataModel.addToDataClasses(child)
        dataModel.addToDataClasses(parent)

        checkAndSave(messageSource, dataModel)

        ReferenceType refType = new ReferenceType(createdBy: DEVELOPMENT, label: 'child')
        child.addToReferenceTypes(refType)
        dataModel.addToDataTypes(refType)

        DataElement el1 = new DataElement(createdBy: DEVELOPMENT, label: 'child', minMultiplicity: 1, maxMultiplicity: 1)
        refType.addToDataElements(el1)
        parent.addToDataElements(el1)

        DataClass content = new DataClass(createdBy: DEVELOPMENT, label: 'content', description: 'A dataclass with elements',
                                          minMultiplicity: 0, maxMultiplicity: 1)
        DataElement el2 = new DataElement(createdBy: DEVELOPMENT, label: 'ele1',
                                          minMultiplicity: 0, maxMultiplicity: 20)
        dataModel.findDataTypeByLabel('string').addToDataElements(el2)
        content.addToDataElements(el2)
        DataElement el3 = new DataElement(createdBy: DEVELOPMENT, label: 'element2',
                                          minMultiplicity: 1, maxMultiplicity: 1)
        dataModel.findDataTypeByLabel('integer').addToDataElements(el3)
        content.addToDataElements(el3)
        dataModel.addToDataClasses(content)

        checkAndSave(messageSource, dataModel)

        SemanticLink link = new SemanticLink(linkType: SemanticLinkType.DOES_NOT_REFINE, createdBy: DEVELOPMENT,
                                             targetCatalogueItem: DataClass.findByLabel('parent'))
        DataClass.findByLabel('content').addToSemanticLinks(link)

        checkAndSave(messageSource, dataModel)
        checkAndSave(messageSource, link)

        dataModel
    }
}
