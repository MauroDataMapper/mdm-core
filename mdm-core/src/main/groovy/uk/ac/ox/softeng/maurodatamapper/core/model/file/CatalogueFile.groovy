package uk.ac.ox.softeng.maurodatamapper.core.model.file

import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import groovy.transform.CompileStatic
import org.springframework.web.multipart.MultipartFile

import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
trait CatalogueFile implements EditHistoryAware {

    @BindUsing({
        obj, source ->
            if (source['fileContents'] instanceof MultipartFile) {
                obj.fileName = obj.fileName ?: ((MultipartFile) source['fileContents']).originalFilename
            }
            source['fileContents']
    })
    byte[] fileContents
    String fileName
    Long fileSize
    String fileType
    /*
        static constraints = {
            createdBy email: true
            fileContents maxSize: 200000000
            fileName blank: false
            fileType blank: false
        }

        def beforeValidate() {

            determineFileType()
        }
    */

    def determineFileType() {
        fileSize = fileContents?.size()
        if (!fileType) {
            if (fileContents) {
                Path temp = Files.createTempFile('cf_', fileName)
                Files.write(temp, fileContents)
                fileType = Files.probeContentType(temp)
            }
            if (fileName) {
                fileType = fileType ?: fileName.contains('.') ? fileName.split(/\./)[-1] : 'Unknown'
            }
            fileType = fileType ?: 'Unknown'
        }
    }

    String getContentType() {
        if (fileType.toLowerCase().contains('png')) return 'image/png'
        if (fileType.toLowerCase().contains('jpg') || fileType.contains('jpeg')) return 'image/jpeg'
        if (fileType.toLowerCase().contains('gif')) return 'image/gif'
        'application/octet-stream'
    }

    @Override
    String getEditLabel() {
        "${getClass().simpleName}:${fileName}"
    }

    //    static DetachedCriteria<CatalogueFile> byCreatedBy(User catalogueUser) {
    //        new DetachedCriteria<CatalogueFile>(CatalogueFile).eq('createdBy', catalogueUser.emailAddress)
    //    }
    //
    static <T extends CatalogueFile> DetachedCriteria<T> withBaseFilter(DetachedCriteria<T> criteria, Map filters) {
        if (filters.fileName) criteria = criteria.ilike('fileName', "%${filters.fileName}%")
        if (filters.fileType) criteria = criteria.ilike('fileType', "%${filters.fileType}%")
        if (filters.fileSize) criteria = criteria.ilike('domainType', "%${filters.fileSize}%")
        criteria
    }
}