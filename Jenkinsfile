pipeline {
    agent any

    environment {
        JENKINS = 'true'
    }

    tools {
        jdk 'jdk-17'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '30'))
    }

    stages {

        stage('Info') {
            parallel {
                stage('Core gradle info') {
                    steps {
                        sh './gradlew -v' // Output gradle version for verification checks
                        sh './gradlew jvmArgs sysProps'
                    }
                }
                stage('mdm-core info') {
                    steps {
                        dir('mdm-core') {
                            sh './grailsw -v' // Output grails version for verification checks
                        }
                    }
                }
            }
        }

    }
}