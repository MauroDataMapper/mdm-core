package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import grails.plugins.metadata.PluginSource

@PluginSource
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        Utils.outputRuntimeArgs(Application)
        GrailsApp.run(Application, args)
    }
}