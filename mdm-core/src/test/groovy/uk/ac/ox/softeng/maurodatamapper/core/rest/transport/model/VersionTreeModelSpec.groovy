package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec
import uk.ac.ox.softeng.maurodatamapper.util.Version

import groovy.util.logging.Slf4j

@Slf4j
class VersionTreeModelSpec extends BaseUnitSpec {

    void 'test ordering v1 to fork'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_FORK_OF,
                                                   m1)

        expect: 'v1, fork'
        m1 <=> m2 == -1
        log.info('{}', [m2, m1].sort())
        [m2, m1].sort() == [m1, m2]
    }

    void 'test ordering v1 to v2'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('2'),
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)

        expect: 'v1, v2'
        m1 <=> m2 == -1
        log.info('{}', [m2, m1].sort())
        [m2, m1].sort() == [m1, m2]
    }

    void 'test ordering v1 to branches'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m3 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'anotherBranch',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)

        expect: 'v1, main, anotherBranch'
        m1 <=> m2 == -1
        m2 <=> m3 == -1
        m1 <=> m3 == -1
        log.info('{}', [m2, m3, m1].sort())
        [m2, m3, m1].sort() == [m1, m2, m3]
    }

    void 'test ordering v1 to fork branches'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m3 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'anotherBranch',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m4 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_FORK_OF,
                                                   m1)

        expect: 'v1, main, anotherBranch, fork'
        m1 < m2
        m2 < m3
        m1 < m3
        m1 < m4
        m4 > m2
        m4 > m3
        log.info('{}', [m4, m2, m3, m1].sort())
        [m4, m2, m3, m1].sort() == [m1, m2, m3, m4]
    }

    void 'test ordering v1,v2 to branches'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('2'),
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m3 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'anotherBranch',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m4 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m2)

        expect: 'v1, anotherBranch, v2, main'
        log.info('{}', [m2, m3, m1, m4].sort())
        [m2, m3, m1, m4].sort() == [m1, m3, m2, m4]
    }

    void 'test ordering v1,v2 to fork branches'() {
        given:
        VersionTreeModel m1 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('1'),
                                                                  documentationVersion: Version.from('1')),
                                                   null,
                                                   null)
        VersionTreeModel m5 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_FORK_OF,
                                                   m1)
        VersionTreeModel m2 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: Version.from('2'),
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m1)
        VersionTreeModel m3 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'anotherBranch',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m2)
        VersionTreeModel m4 = new VersionTreeModel(new BasicModel(id: UUID.randomUUID(),
                                                                  branchName: 'main',
                                                                  modelVersion: null,
                                                                  documentationVersion: Version.from('1')),
                                                   VersionLinkType.NEW_MODEL_VERSION_OF,
                                                   m2)

        expect: 'v1, fork, v2, main, anotherBranch'
        log.info('{}', [m2, m3, m1, m4, m5].sort())
        [m2, m3, m1, m4, m5].sort() == [m1, m5, m2, m4, m3]
    }
}
