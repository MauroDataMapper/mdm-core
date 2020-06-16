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
        timeout(time: 30, unit: 'MINUTES')
        skipStagesAfterUnstable()
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
                stage('mdm-plugin-email-proxy info') {
                    steps {
                        dir('mdm-plugin-email-proxy') {
                            sh './grailsw -v' // Output grails version for verification checks
                        }
                    }
                }
                stage('mdm-plugin-datamodel info') {
                    steps {
                        dir('mdm-plugin-datamodel') {
                            sh './grailsw -v' // Output grails version for verification checks
                        }
                    }
                }
                stage('mdm-security info') {
                    steps {
                        dir('mdm-security') {
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

        stage('Parallel Unit Test') {
            parallel {
                stage('mdm-core') {
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
                stage('mdm-plugin-email-proxy') {
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
                stage('mdm-plugin-datamodel') {
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
                stage('mdm-security') {
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
            }
        }

        stage('Parallel Integration Test') {
            parallel {
                stage('mdm-core') {
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
                stage('mdm-plugin-email-proxy') {
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
                stage('mdm-plugin-datamodel') {
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
                stage('mdm-security') {
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
            }
        }

        stage('Parallel Functional Test') {
            parallel {
                stage('mdm-core') {
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
                stage('mdm-plugin-datamodel') {
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
                stage('mdm-security') {
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