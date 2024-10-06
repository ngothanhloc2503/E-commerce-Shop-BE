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
                        sh "docker build --no-cache --rm -t ${DOCKER_IMAGE} ."
                        sh "docker push ${DOCKER_IMAGE}"
                        sh "docker rmi ${DOCKER_IMAGE}" // Remove the image after pushing to save disk space
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    withCredentials([sshUserPrivateKey(credentialsId: 'ec2-app-server-ssh-credentials-id', keyFileVariable: 'SSH_KEY'), usernamePassword(credentialsId: 'harbor-credentials-id', passwordVariable: 'HARBOR_PASSWORD', usernameVariable: 'HARBOR_USERNAME')]) {
                        sh """
                            ssh -i ${env.SSH_KEY} -o StrictHostKeyChecking=no ubuntu@47.129.222.124 '
                                echo ${HARBOR_PASSWORD} | docker login ${HARBOR_REGISTRY} -u ${HARBOR_USERNAME} --password-stdin &&
                                docker pull ${DOCKER_IMAGE} &&
                                docker stop ${APP_NAME} || true &&
                                docker rm -f ${APP_NAME} || true &&
                                docker run -d --name ${APP_NAME} -p 5432:5432 ${DOCKER_IMAGE}
                            '
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs() // Clean workspace after build to free space
        }
    }
}