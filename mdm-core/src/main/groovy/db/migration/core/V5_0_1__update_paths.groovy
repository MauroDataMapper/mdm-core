package db.migration.core

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context


@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        Folder.withNewTransaction {
            List<Folder> folders = Folder.byNoParentFolder().list()
            folders.each { f ->
                checkFolder(f)
            }
        }

        Classifier.withNewTransaction {
            List<Classifier> classifiers = Classifier.byNoParentClassifier().list()
            classifiers.each {c ->
                checkClassifier(c)
            }
        }

        Annotation.withNewTransaction {
            List<Annotation> annotations = Annotation.byNoParentAnnotation().list()
            annotations.each { a ->
                checkAnnotation(a)
            }
        }
    }

    void checkFolder(Folder folder) {
        folder.checkPath()
        Folder.byParentFolderId(folder.id).list().each { f ->
            checkFolder(f)
        }
    }

    void checkClassifier(Classifier classifier) {
        classifier.checkPath()
        Classifier.byParentClassifierId(classifier.id).list().each { c ->
            checkClassifier(c)
        }
    }

    void checkAnnotation(Annotation annotation) {
        annotation.checkPath()
        Annotation.byParentAnnotationId(annotation.id).list().each { a ->
            checkAnnotation(a)
        }
    }
}
