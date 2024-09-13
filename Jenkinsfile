pipeline {
    agent any

    tools {
        maven 'Maven 3.x'
    }

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
                // Use Maven to clean and build the project
                sh "mvn clean install"
            }
        }

        stage('Test') {
            steps {
                // Run unit tests
                sh "mvn test"
            }
        }

        stage('Package') {
            steps {
                // Package the application into a JAR file
                sh "mvn package"
            }
        }

        stage('Deploy') {
            steps {
                // This is a placeholder for deployment steps. Customize based on your environment.
                echo 'Deploying the application...'
                // Example: scp the JAR to a server or deploy to a cloud service
                // sh 'scp target/myapp.jar user@server:/path/to/deploy/'
            }
        }
    }

    post {
        always {
            // Clean up workspace after the job is done
            cleanWs()
        }

        success {
            echo 'Pipeline completed successfully!'
        }

        failure {
            echo 'Pipeline failed!'
        }
    }
}