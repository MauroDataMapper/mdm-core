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
                        sh './gradlew jvmArgs'
                        sh './gradlew sysProps'
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
                sh './gradlew compile'
            }
        }

        stage('License Header Check') {
            steps {
                warnError('Missing License Headers') {
                    sh './gradlew license'
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
                    sh "./gradlew artifactoryPublish"
                }
            }
        }

        /*
        Unit Tests
         */
        stage('Unit Test: mdm-common') {
            steps {
                sh "./gradlew :mdm-common:test"
            }
            post {
                always {
                    dir('mdm-common') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-core') {
            steps {
                dir('mdm-core') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-core') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-email-proxy') {
            steps {
                dir('mdm-plugin-email-proxy') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-email-proxy') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-datamodel') {
            steps {
                dir('mdm-plugin-datamodel') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-datamodel') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-terminology') {
            steps {
                dir('mdm-plugin-terminology') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-terminology') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-security') {
            steps {
                dir('mdm-security') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-security') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-authentication-basic') {
            steps {
                dir('mdm-plugin-authentication-basic') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-authentication-basic') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-dataflow') {
            steps {
                dir('mdm-plugin-dataflow') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-dataflow') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }
        stage('Unit Test: mdm-plugin-referencedata') {
            steps {
                dir('mdm-plugin-referencedata') {
                    sh "./grailsw test-app -unit"
                }
            }
            post {
                always {
                    dir('mdm-plugin-referencedata') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    }
                }
            }
        }

        /*
        Integration Tests
         */
        stage('Integration Test: mdm-core') {
            steps {
                dir('mdm-core') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-core') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }
        stage('Integration Test: mdm-plugin-email-proxy') {
            steps {
                dir('mdm-plugin-email-proxy') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-email-proxy') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }
        stage('Integration Test: mdm-plugin-datamodel') {
            steps {
                dir('mdm-plugin-datamodel') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-datamodel') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }

        stage('Integration Test: mdm-plugin-terminology') {
            steps {
                dir('mdm-plugin-terminology') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-terminology') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }

        stage('Integration Test: mdm-security') {
            steps {
                dir('mdm-security') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-security') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }
        stage('Integration Test: mdm-plugin-dataflow') {
            steps {
                dir('mdm-plugin-dataflow') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-dataflow') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }
        stage('Integration Test: mdm-plugin-referencedata') {
            steps {
                dir('mdm-plugin-referencedata') {
                    sh "./grailsw -Dgrails.integrationTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-referencedata') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/integrationTest/*.xml'
                    }
                }
            }
        }

        /*
        Functional Tests
         */
        stage('Functional Test: mdm-core') {
            steps {
                dir('mdm-core') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-core') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-security') {
            steps {
                dir('mdm-security') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-security') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-plugin-datamodel') {
            steps {
                dir('mdm-plugin-datamodel') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-datamodel') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-plugin-terminology') {
            steps {
                dir('mdm-plugin-terminology') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-terminology') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-plugin-authentication-basic') {
            steps {
                dir('mdm-plugin-authentication-basic') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-authentication-basic') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-plugin-dataflow') {
            steps {
                dir('mdm-plugin-dataflow') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-dataflow') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }
        stage('Functional Test: mdm-plugin-referencedata') {
            steps {
                dir('mdm-plugin-referencedata') {
                    sh "./grailsw -Dgrails.functionalTest=true test-app -integration"
                }
            }
            post {
                always {
                    dir('mdm-plugin-referencedata') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/functionalTest/*.xml'
                    }
                }
            }
        }

        /*
        E2E Functional Tests
        */
        stage('Core Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=core test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/**/*.xml'
                    }
                }
            }
        }
        stage('Security Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=security test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/core/*.xml'
                    }
                }
            }
        }
        stage('Authentication Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=authentication test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/authentication/*.xml'
                    }
                }
            }
        }
        stage('DataModel Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=datamodel test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/datamodel/*.xml'
                    }
                }
            }
        }
        stage('Terminology Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=terminology test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/terminology/*.xml'
                    }
                }
            }
        }
        stage('DataFlow Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=dataflow test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/dataflow/*.xml'
                    }
                }
            }
        }
        stage('ReferenceData Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.package=referencedata test-app"
                }
            }
            post {
                always {
                    dir('mdm-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/dataflow/*.xml'
                    }
                }
            }
        }
        stage('Trouble Functional Test') {
            steps {
                dir('mdm-testing-functional') {
                    sh "./grailsw -Dgrails.test.category=TroubleTest test-app"
                }
            }
            post {
                always {
                    dir('mc-testing-functional') {
                        junit allowEmptyResults: true, testResults: 'build/test-results/TroubleTest/*.xml'
                    }
                }
            }
        }

        stage('Deploy to Artifactory') {
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
                    sh "./gradlew artifactoryPublish"
                }
            }
        }
    }

    post {
        unstable {
            script {
                sh "./gradlew rootTestReport"
            }
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
        always {
            outputTestResults()
            jacoco execPattern: '**/build/jacoco/*.exec'
            archiveArtifacts allowEmptyArchive: true, artifacts: '**/*.log'
            slackNotification()
        }
    }
}