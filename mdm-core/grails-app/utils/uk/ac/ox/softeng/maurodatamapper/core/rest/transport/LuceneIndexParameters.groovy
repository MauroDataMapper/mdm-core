package uk.ac.ox.softeng.maurodatamapper.core.rest.transport

/**
 * @since 26/04/2018
 */
class LuceneIndexParameters extends UserCredentials {

    Integer typesToIndexInParallel
    Integer threadsToLoadObjects
    Integer batchSizeToLoadObjects
    String cacheMode
    Boolean optimizeOnFinish
    Boolean optimizeAfterPurge
    Boolean purgeAllOnStart
    Integer idFetchSize
    Integer transactionTimeout

    void updateFromMap(Map map) {
        typesToIndexInParallel = typesToIndexInParallel ?: map.typesToIndexInParallel
        threadsToLoadObjects = threadsToLoadObjects ?: map.threadsToLoadObjects
        batchSizeToLoadObjects = batchSizeToLoadObjects ?: map.batchSizeToLoadObjects
        cacheMode = cacheMode ?: map.cacheMode
        optimizeOnFinish = optimizeOnFinish ?: map.optimizeOnFinish
        optimizeAfterPurge = optimizeAfterPurge ?: map.optimizeAfterPurge
        purgeAllOnStart = purgeAllOnStart ?: map.purgeAllOnStart
        idFetchSize = idFetchSize ?: map.idFetchSize
        transactionTimeout = transactionTimeout ?: map.transactionTimeout
    }

    @Override
    String toString() {
        'Lucene Mass Indexer Parameters:\n' +
        "  typesToIndexInParallel: $typesToIndexInParallel\n" +
        "  threadsToLoadObjects: $threadsToLoadObjects\n" +
        "  batchSizeToLoadObjects: $batchSizeToLoadObjects\n" +
        "  cacheMode: $cacheMode\n" +
        "  optimizeOnFinish: $optimizeOnFinish\n" +
        "  optimizeAfterPurge: $optimizeAfterPurge\n" +
        "  purgeAllOnStart: $purgeAllOnStart\n" +
        "  idFetchSize: $idFetchSize\n" +
        "  transactionTimeout: $transactionTimeout"
    }
}
