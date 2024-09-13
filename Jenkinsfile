pipeline {
    agent any

    environment {
        // Define environment variables for tools and paths
        MAVEN_HOME = tool name: 'Maven 3.11.0', type: 'maven'
        JAVA_HOME = tool name: 'JDK 17', type: 'jdk'
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
                sh "'${MAVEN_HOME}/bin/mvn' clean install"
            }
        }

        stage('Test') {
            steps {
                // Run unit tests
                sh "'${MAVEN_HOME}/bin/mvn' test"
            }

            post {
                // Publish test results (JUnit)
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                // Package the application into a JAR file
                sh "'${MAVEN_HOME}/bin/mvn' package"
            }

            post {
                success {
                    echo 'Build and package successful!'
                    archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: false
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'master' // Deploy only on the master branch
            }
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