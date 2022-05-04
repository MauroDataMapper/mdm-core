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
        //        timeout(time: 45, unit: 'MINUTES')
        //        skipStagesAfterUnstable()
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

        // If the flyway is broken then do NOT deploy to artifactory
        stage('Flyway Migration Check') {
            steps {
                sh './gradlew --build-cache verifyFlywayMigrationVersions'
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
                    sh "./gradlew --build-cache publish"
                }
            }
        }


        /**
         * Parallel jenkins vs non-parallel jenkins testing
         *
         * When gradle runs a "test executor" with parallel disabled it will only use 1CPU which means we can make use of jenkins
         * to parallelise the tests.
         *
         * We cannot use gradle to parallelise as it will run up 1 instance and use all the same variables which means items like the HS directory get broken
         * and the "state" of the instance is not the same.
         *
         * Using serial execution currently takes
         *
         * ~12mins for ITs
         * ~21mins for FTs
         * ~2hrs for E2Es
         *
         * Changing to parallel execution for each of the sections
         *
         * ~10mins for ITs
         * ~18mins for FTs
         * ~45mins for E2Es
         *
         * Whilst it looks like some of the jobs take much longer it is because jenkins start the stage time when the stage starts, and then it keeps counting while the
         * parallel stage sits waiting for a free executor
         */


        /*
        Unit Tests
         */
        stage('Unit Tests') {
            // Dont run these on main branch
            when {
                not {
                    branch 'main'
                }
            }
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
            // Dont run these on main branch
            when {
                not {
                    branch 'main'
                }
            }
            parallel {
                stage('Parallel Tests') {
                    // These are tests which can be run in gradle parallel model
                    steps {
                        sh './gradlew --build-cache -Dgradle.integrationTest=true  -Dgradle.parallel=true ' + [
                            'mdm-core',
                            'mdm-plugin-authentication-apikey',
                            'mdm-plugin-authentication-basic',
                            'mdm-plugin-dataflow',
                            'mdm-plugin-datamodel',
                            'mdm-plugin-email-proxy',
                            'mdm-plugin-federation',
                            'mdm-plugin-profile',
                            'mdm-plugin-referencedata',
                            'mdm-plugin-terminology',
//                            'mdm-security',
                        ].collect {":${it}:integrationTest"}.join(' ')
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/build/test-results/parallelIntegrationTest/*.xml'
                        }
                    }
                }
                stage('Non-Parallel Tests') {
                    // These are tests which cannot be run in gradle parallel model - currently any test which needs to use the lucene index directory
                    steps {
                        sh './gradlew --build-cache -Dgradle.integrationTest=true -Dgradle.nonParallel=true ' + [
                            'mdm-core',
                            'mdm-plugin-authentication-apikey',
                            'mdm-plugin-authentication-basic',
                            'mdm-plugin-dataflow',
                            'mdm-plugin-datamodel',
                            'mdm-plugin-email-proxy',
                            'mdm-plugin-federation',
                            'mdm-plugin-profile',
                            'mdm-plugin-referencedata',
                            'mdm-plugin-terminology',
                            'mdm-security',
                        ].collect {":${it}:integrationTest"}.join(' ')
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: '**/build/test-results/nonParallelIntegrationTest/*.xml'
                        }
                    }
                }
            }
        }


        /*
        Functional Tests
         */
        stage('Functional Tests') {
            // Dont run these on main branch
            when {
                not {
                    branch 'main'
                }
            }
            parallel {
                stage('Functional Test: mdm-core') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-core:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-core/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-authentication-apikey') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-authentication-apikey:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-authentication-apikey/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-authentication-basic') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-authentication-basic:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-authentication-basic/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-dataflow') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-dataflow:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-dataflow/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-datamodel') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-datamodel:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-datamodel/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-federation') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-federation:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-federation/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-profile') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-profile:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-profile/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-referencedata') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-referencedata:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-referencedata/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-plugin-terminology') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-plugin-terminology:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-plugin-terminology/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
                stage('Functional Test: mdm-security') {
                    steps {
                        sh "./gradlew -Dgradle.functionalTest=true :mdm-security:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-security/build/test-results/functionalTest/*.xml'
                        }
                    }
                }
            }

        }

        /*
        E2E Functional Tests
        */
        stage('E2E Functional Tests') {
            parallel {
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
                stage('E2E Profile Functional Test') {
                    steps {
                        sh "./gradlew -Dgradle.test.package=profile :mdm-testing-functional:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/profile/*.xml'
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
                stage('E2E Federation Functional Test') {
                    steps {
                        sh "./gradlew -Dgradle.test.package=federation :mdm-testing-functional:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/federation/*.xml'
                        }
                    }
                }
                stage('E2E Facets Functional Test') {
                    steps {
                        sh "./gradlew -Dgradle.test.package=facet :mdm-testing-functional:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/facet/*.xml'
                        }
                    }
                }
                stage('E2E Versioned Folder Functional Test') {
                    steps {
                        sh "./gradlew -Dgradle.test.package=versionedfolder :mdm-testing-functional:integrationTest"
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'mdm-testing-functional/build/test-results/versionedfolder/*.xml'
                        }
                    }
                }
            }
        }

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

        /*
        Static Code Analysis
        */
        stage('Static Code Analysis') {
            steps {
                sh "./gradlew -PciRun=true jacocoTestReport"
                sh "./gradlew -PciRun=true staticCodeAnalysis"
            }
        }

        /*
        Sonarqube report
        */
        stage('Sonarqube') {
            when {
                branch 'develop'
            }
            steps {
                withSonarQubeEnv('JenkinsQube') {
                    sh "./gradlew sonarqube"
                }
            }
        }

        stage('Continuous Deployment') {
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
                    try {
                        println("Triggering the [continuous-deployment] job")
                        build quietPeriod: 300, wait: false, job: 'continuous-deployment'
                    } catch (hudson.AbortException ignored) {
                        println("Cannot trigger the [continuous-deployment] job as it doesn't exist")
                    }
                }
            }
        }

        stage('Deploy main to Artifactory') {
            when {
                allOf {
                    branch 'main'
                    expression {
                        currentBuild.currentResult == 'SUCCESS'
                    }
                }
            }
            steps {
                script {
                    sh "./gradlew --build-cache publish"
                }
            }
        }
    }

    post {
        always {
            recordIssues enabledForFailure: true, tools: [java(), javaDoc()]
            recordIssues enabledForFailure: true, tool: checkStyle(pattern: '**/reports/checkstyle/*.xml')
            recordIssues enabledForFailure: true, tool: codeNarc(pattern: '**/reports/codenarc/*.xml')
            recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/reports/spotbugs/*.xml', useRankAsPriority: true)
            recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/reports/pmd/*.xml')

            publishCoverage adapters: [jacocoAdapter('**/reports/jacoco/jacocoTestReport.xml')]
            outputTestResults()
            jacoco classPattern: '**/build/classes', execPattern: '**/build/jacoco/*.exec', sourceInclusionPattern: '**/*.java,**/*.groovy',
                   sourcePattern: '**/src/main/groovy,**/grails-app/controllers,**/grails-app/domain,**/grails-app/services,**/grails-app/utils'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
            zulipNotification(topic: 'mdm-core')
        }
    }
}