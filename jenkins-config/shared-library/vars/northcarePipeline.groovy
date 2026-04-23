/**
 * northcarePipeline — shared pipeline entrypoint for all NorthCare microservices.
 *
 * Usage in service Jenkinsfile:
 *   @Library('northcare-pipeline') _
 *   northcarePipeline(serviceName: 'hospital-core')
 */
def call(Map config = [:]) {
    def serviceName = config.serviceName ?: error("northcarePipeline: 'serviceName' is required")
    def ecrRegistry = config.ecrRegistry ?: env.ECR_REGISTRY_URL
    def region      = config.region      ?: 'us-east-1'

    if (!ecrRegistry) {
        error("northcarePipeline: ECR_REGISTRY_URL not set and 'ecrRegistry' not provided")
    }

    pipeline {
        agent {
            label 'docker'
        }

        options {
            timeout(time: 30, unit: 'MINUTES')
            buildDiscarder(logRotator(numToKeepStr: '30'))
            disableConcurrentBuilds(abortPrevious: true)
            timestamps()
            ansiColor('xterm')
        }

        environment {
            SERVICE_NAME  = "${serviceName}"
            IMAGE_TAG     = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7) ?: 'unknown'}"
            ECR_IMAGE     = "${ecrRegistry}/northcare/${serviceName}:${env.IMAGE_TAG}"
            ECR_IMAGE_LATEST = "${ecrRegistry}/northcare/${serviceName}:latest"
            AWS_REGION    = "${region}"
            DOCKER_BUILDKIT = "1"
        }

        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                    script {
                        env.GIT_COMMIT_SHORT = sh(
                            script: 'git rev-parse --short HEAD',
                            returnStdout: true
                        ).trim()
                        env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                        env.ECR_IMAGE = "${ecrRegistry}/northcare/${serviceName}:${env.IMAGE_TAG}"
                        echo "Building image: ${env.ECR_IMAGE}"
                    }
                }
            }

            stage('Lint & Test') {
                steps {
                    script {
                        northcareTest(serviceName: serviceName)
                    }
                }
                post {
                    always {
                        junit allowEmptyResults: true, testResults: '**/test-results/**/*.xml'
                        publishHTML([
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'coverage',
                            reportFiles: 'index.html',
                            reportName: 'Coverage Report'
                        ])
                    }
                }
            }

            stage('Docker Build') {
                steps {
                    script {
                        northcareBuild(
                            image: env.ECR_IMAGE,
                            serviceName: serviceName
                        )
                    }
                }
            }

            stage('Security Scan') {
                steps {
                    script {
                        northcareScan(image: env.ECR_IMAGE)
                    }
                }
            }

            stage('Push to ECR') {
                steps {
                    script {
                        northcarePush(
                            image: env.ECR_IMAGE,
                            latestImage: env.ECR_IMAGE_LATEST,
                            region: region
                        )
                    }
                }
            }

            stage('Update GitOps') {
                when {
                    branch 'main'
                    not { changeRequest() }
                }
                steps {
                    script {
                        northcareGitOpsUpdate(
                            serviceName: serviceName,
                            imageTag: env.IMAGE_TAG
                        )
                    }
                }
            }
        }

        post {
            success {
                echo "✅ ${serviceName}:${env.IMAGE_TAG} built and deployed via GitOps"
            }
            failure {
                echo "❌ Pipeline failed for ${serviceName} — check logs above"
            }
            always {
                cleanWs(
                    cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true
                )
            }
        }
    }
}
