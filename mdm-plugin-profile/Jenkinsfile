pipeline {
    agent any

    tools {
        jdk 'jdk-12'
    }

    options {
        timestamps()
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

        stage('Compile') {
            steps {
                sh './gradlew -v' // Output gradle version for verification checks
                sh "./gradlew jenkinsClean compile"
            }
        }

        stage('Test') {
            steps {
                sh "./grailsw test-app"
            }
            post {
                always {
                    publishHTML([
                        allowMissing         : false,
                        alwaysLinkToLastBuild: true,
                        keepAll              : true,
                        reportDir            : 'build/reports/tests',
                        reportFiles          : 'index.html',
                        reportName           : 'Integration Test Report',
                        reportTitles         : 'Test'
                    ])
                    junit allowEmptyResults: true, testResults: '**/build/test-results/**/*.xml'
                    outputTestResults()
                }
            }
        }

        stage('License Header Check'){
            steps{
                sh './gradlew license'
            }
        }

        stage('Deploy to Artifactory') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
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
        always {
            slackNotification()
        }
    }
}
