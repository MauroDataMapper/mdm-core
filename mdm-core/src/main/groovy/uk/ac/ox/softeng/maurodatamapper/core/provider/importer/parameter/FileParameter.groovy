package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter

/**
 * @since 06/03/2018
 */
class FileParameter {

    byte[] fileContents
    String fileName
    String fileType

    FileParameter() {

    }

    FileParameter(String fileName, String fileType, byte[] fileContents) {
        this.fileName = fileName
        this.fileType = fileType
        this.fileContents = fileContents.clone()
    }

    byte[] getFileContents() {
        fileContents.clone()
    }

    void setFileContents(byte[] fileContents) {
        this.fileContents = fileContents.clone()
    }

    InputStream getInputStream() {
        new ByteArrayInputStream(fileContents)
    }
}
