pipeline {
    agent any

    environment {
        JENKINS = 'true'
    }

    tools {
        jdk 'jdk-12'
    }

    options {
        timestamps()
        //        timeout(time: 45, unit: 'MINUTES')
        // skipStagesAfterUnstable()
        buildDiscarder(logRotator(numToKeepStr: '30'))
        disableConcurrentBuilds()
        throttleJobProperty(
            categories: ['mdm-core'],
            throttleEnabled: true,
            throttleOption: 'category'
        )
    }

    stages {

        stage('Clean') {
            // Only clean when the last build failed
            when {
                expression {
                    currentBuild.previousBuild?.currentResult == 'FAILURE'
                }
            }
            steps {
                sh "./gradlew clean"
            }
        }

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

        stage('Test cleanup & Compile') {
            steps {
                sh "./gradlew jenkinsClean"
                sh './gradlew --build-cache compile'
            }
        }

        stage('License Header Check') {
            steps {
                warnError('Missing License Headers') {
                    sh './gradlew --build-cache license'
                }
            }
        }

        // Deploy develop branch even if tests fail if the code builds, as it'll be an unstable snapshot but we should still deploy
        stage('Deploy develop to Artifactory') {
            when {
                allOf {
                    branch 'develop'
                    expression {
                        currentBuild.currentResult == 'SUCCESS'
                    }
                }

            }
            steps {
                script {
                    sh "./gradlew --build-cache artifactoryPublish"
                }
            }
        }

        /*
        Unit Tests
         */
        stage('Unit Tests') {
            steps {
                sh "./gradlew --build-cache test"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
                }
            }
        }

        /*
        Integration Tests
         */
        stage('Integration Tests') {
            steps {
                sh './gradlew --build-cache -Dgradle.integrationTest=true ' + [
                    'mdm-core',
                    //                    'mdm-plugin-authentication-apikey',
                    //                    'mdm-plugin-authentication-basic',
                    'mdm-plugin-dataflow',
                    'mdm-plugin-datamodel',
                    'mdm-plugin-email-proxy',
                    //                    'mdm-plugin-profile',
                    'mdm-plugin-referencedata',
                    'mdm-plugin-terminology',
                    'mdm-security',
                ].collect {":${it}:integrationTest"}.join(' ')
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/build/test-results/integrationTest/*.xml'
                }
            }
        }

        /*
        Functional Tests
         */
        stage('Functional Test: mdm-core') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-core:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-core/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-authentication-apikey') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-authentication-apikey:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-authentication-apikey/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-authentication-basic') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-authentication-basic:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-authentication-basic/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-dataflow') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-dataflow:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-dataflow/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-datamodel') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-datamodel:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-datamodel/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-referencedata') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-referencedata:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-referencedata/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-terminology') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-terminology:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-terminology/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-security') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-security:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-security/build/test-results/functionalTest/*.xml'
                }
            }
        }
        stage('Functional Test: mdm-plugin-profile') {
            steps {
                sh "./gradlew -Dgrails.functionalTest=true :mdm-plugin-profile:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-plugin-profile/build/test-results/functionalTest/*.xml'
                }
            }
        }

        /*
        E2E Functional Tests
        */
        stage('E2E Core Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=core :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/core/*.xml'
                }
            }
        }
        stage('E2E Security Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=security :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/security/*.xml'
                }
            }
        }
        stage('E2E Authentication Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=authentication :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/authentication/*.xml'
                }
            }
        }
        stage('E2E DataModel Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=datamodel :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/datamodel/*.xml'
                }
            }
        }
        stage('E2E Terminology Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=terminology :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/terminology/*.xml'
                }
            }
        }
        stage('E2E DataFlow Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=dataflow :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/dataflow/*.xml'
                }
            }
        }
        stage('E2E ReferenceData Functional Test') {
            steps {
                sh "./gradlew -Dgradle.test.package=referencedata :mdm-testing-functional:integrationTest"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/referencedata/*.xml'
                }
            }
        }
//        stage('E2E Profile Functional Test') {
//            steps {
//                sh "./gradlew -Dgradle.test.package=profile :mdm-testing-functional:integrationTest"
//            }
//            post {
//                always {
//                    junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/profile/*.xml'
//                }
//            }
//        }

        stage('Compile complete Test Report') {
            steps {
                sh "./gradlew --build-cache rootTestReport"
            }
            post {
                always {
                    publishHTML([
                        allowMissing         : false,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests',
                        reportFiles          : 'index.html',
                        reportName           : 'Complete Test Report',
                        reportTitles         : 'Test'
                    ])
                }
            }
        }

        stage('Deploy master to Artifactory') {
            when {
                allOf {
                    branch 'master'
                    expression {
                        currentBuild.currentResult == 'SUCCESS'
                    }
                }
            }
            steps {
                script {
                    sh "./gradlew --build-cache artifactoryPublish"
                }
            }
        }
    }

    post {
        always {
            outputTestResults()
            jacoco execPattern: '**/build/jacoco/*.exec'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
            slackNotification()
        }
    }
}