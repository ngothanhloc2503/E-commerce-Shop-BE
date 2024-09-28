pipeline {
    agent any

    environment {
        HARBOR_REGISTRY = 'registry-ntloc.ddns.net'
        APP_NAME='e-commerce-shop-be'
        GITLAB_REPO = 'https://gitlab.com/ntloc2503/e-commerce-shop-be.git'
        DOCKER_IMAGE = "${HARBOR_REGISTRY}/e-commerce-shop/${APP_NAME}:latest"
        COMMIT_HASH = ''
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                    git branch: 'master',
                        credentialsId: 'gitlab-credentials-id',
                        url: "${GITLAB_REPO}"

                    // Get the short GitLab commit hash
                    COMMIT_HASH = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    DOCKER_IMAGE = "${HARBOR_REGISTRY}/e-commerce-shop/${APP_NAME}:${COMMIT_HASH}"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests=true'
            }
        }

        stage('Push to Harbor') {
            steps {
                script {
                    // Log in to the Harbor registry
                    withCredentials([usernamePassword(credentialsId: 'harbor-credentials-id', passwordVariable: 'HARBOR_PASSWORD', usernameVariable: 'HARBOR_USERNAME')]) {
                        sh "echo ${HARBOR_PASSWORD} | docker login ${HARBOR_REGISTRY} -u ${HARBOR_USERNAME} --password-stdin"
                        sh "docker build -t ${DOCKER_IMAGE} ."
                        sh "docker push ${DOCKER_IMAGE}"
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                echo 'Deploying the application...'
            }
        }
    }
}