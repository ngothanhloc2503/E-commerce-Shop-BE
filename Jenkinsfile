pipeline {
    agent any

    stages {
        stage('Checkout Code') {
            steps {
                // Checkout the code from the repository (update with your repo details)
                git branch: 'master',
                credentialsId: 'gitlab-token',
                url: 'https://gitlab.com/ntloc2503/e-commerce-shop-be.git'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install'
                sh 'docker build -t ecommerce-shop-be .'
            }
        }

        stage('Push to Docker Hub') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKERHUB_CREDENTIALS') {
                        sh 'docker push ecommerce-shop-be'
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