/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
