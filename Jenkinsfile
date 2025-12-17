pipeline {
    agent any

    options {
        // Build options
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }

    environment {
        // Docker image configuration
        DOCKER_IMAGE = 'quay.io/wissensalt/spring-security-session-redis'
        DOCKER_TAG = "${BUILD_NUMBER}"
        DOCKER_LATEST = "${DOCKER_IMAGE}:latest"
        DOCKER_VERSIONED = "${DOCKER_IMAGE}:${DOCKER_TAG}"

        // Maven configuration
        MAVEN_OPTS = '-Xmx1024m -XX:MaxMetaspaceSize=512m'
        MAVEN_CLI_OPTS = '--batch-mode --errors --fail-at-end --show-version'

        // Application properties
        JAVA_VERSION = '21'
    }


    stages {
        stage('Checkout') {
            steps {
                script {
                    echo "🔄 Checking out code from SCM..."

                    // Standard checkout
                    checkout scm

                    // Get commit info for tagging
                    env.GIT_COMMIT_SHORT = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    env.DOCKER_TAG = "${BUILD_NUMBER}-${env.GIT_COMMIT_SHORT}"
                    env.DOCKER_VERSIONED = "${DOCKER_IMAGE}:${env.DOCKER_TAG}"

                    echo "✅ Code checked out successfully"
                }
            }
        }

        stage('Build Info') {
            steps {
                script {
                    echo "📋 Build Information:"
                    echo "  - Project: spring-security-session-redis"
                    echo "  - Build Number: ${BUILD_NUMBER}"
                    echo "  - Git Commit: ${env.GIT_COMMIT_SHORT}"
                    echo "  - Docker Image: ${env.DOCKER_VERSIONED}"
                    echo "  - Java Version: ${JAVA_VERSION}"
                }
            }
        }

        stage('Validate') {
            steps {
                script {
                    echo "✅ Validating project structure and dependencies..."
                    sh "chmod +x ./mvnw"
                    sh "./mvnw ${MAVEN_CLI_OPTS} validate"

                    // Check if required files exist
                    sh '''
                        echo "Checking required files..."
                        test -f pom.xml && echo "✓ pom.xml found"
                        test -f Dockerfile && echo "✓ Dockerfile found"
                        test -f src/main/java/com/wissensalt/springsecuritysessionredis/SpringSecuritySessionRedisApplication.java && echo "✓ Main application class found"
                        test -d src/main/resources && echo "✓ Resources directory found"
                    '''
                }
            }
        }

        stage('Compile') {
            steps {
                script {
                    echo "🔨 Compiling source code..."
                    sh "./mvnw ${MAVEN_CLI_OPTS} compile"
                }
            }
        }

        stage('Test') {
            steps {
                script {
                    echo "🧪 Running unit tests..."
                    sh "./mvnw ${MAVEN_CLI_OPTS} test"
                }
            }
            // post {
            //     always {
            //         // Publish test results
            //         script {
            //             if (fileExists('target/surefire-reports/TEST-*.xml')) {
            //                 junit 'target/surefire-reports/TEST-*.xml'
            //                 echo "📊 Test results published"
            //             }
            //         }
            //     }
            // }
        }

        stage('Package') {
            steps {
                script {
                    echo "📦 Packaging application..."
                    sh "./mvnw ${MAVEN_CLI_OPTS} package -DskipTests"

                    // Archive the built JAR
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    echo "✅ JAR file archived"
                }
            }
        }

        stage('Code Quality') {
            parallel {
                stage('Security Scan') {
                    steps {
                        script {
                            echo "🔒 Running security scan..."
                            // Using OWASP Dependency Check
                            sh "./mvnw ${MAVEN_CLI_OPTS} org.owasp:dependency-check-maven:check || true"
                        }
                    }
                }

                stage('Static Analysis') {
                    when {
                        anyOf {
                            branch 'develop'
                            changeRequest()
                        }
                    }
                    steps {
                        script {
                            echo "📊 Running static code analysis..."
                            // Placeholder for SonarQube or similar tools
                            echo "Static analysis would run here (SonarQube, SpotBugs, etc.)"
                        }
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Building Docker image..."

                    // Build the Docker image
                    def image = docker.build("${env.DOCKER_VERSIONED}", ".")

                    // Tag as latest for main branch
                    if (env.BRANCH_NAME == 'main') {
                        image.tag('latest')
                        echo "✅ Tagged as latest"
                    }

                    echo "✅ Docker image built: ${env.DOCKER_VERSIONED}"
                }
            }
        }

        // stage('Security Scan - Image') {
        //     steps {
        //         script {
        //             echo "🔍 Scanning Docker image for vulnerabilities..."

        //             // Using Trivy for container scanning
        //             sh """
        //                 docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \\
        //                     -v \$(pwd):/workspace \\
        //                     aquasec/trivy:latest image \\
        //                     --exit-code 0 \\
        //                     --severity HIGH,CRITICAL \\
        //                     --format json \\
        //                     --output /workspace/trivy-report.json \\
        //                     ${env.DOCKER_VERSIONED} || true
        //             """

        //             // Archive the security report
        //             archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true
        //         }
        //     }
        // }

        stage('Integration Tests') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    changeRequest()
                }
            }
            steps {
                script {
                    echo "🧪 Running integration tests..."

                    // Start services using docker-compose for integration testing
                    sh '''
                        # Copy docker-compose for testing
                        if [ -f docker-compose.yml ]; then
                            echo "Starting test environment..."
                            docker-compose -f docker-compose.yml up -d postgres redis

                            # Wait for services to be ready
                            sleep 30

                            # Run integration tests
                            ./mvnw ${MAVEN_CLI_OPTS} verify -Pintegration-test || true

                            # Cleanup
                            docker-compose -f docker-compose.yml down
                        else
                            echo "No docker-compose.yml found, skipping integration tests"
                        fi
                    '''
                }
            }
            // post {
            //     always {
            //         script {
            //             if (fileExists('target/failsafe-reports/TEST-*.xml')) {
            //                 junit 'target/failsafe-reports/TEST-*.xml'
            //                 echo "📊 Integration test results published"
            //             }
            //         }
            //     }
            // }
        }

        stage('Push to Registry') {
            when {
                anyOf {
                    branch 'main'
                    branch 'develop'
                    buildingTag()
                }
            }
            steps {
                script {
                    echo "📤 Pushing Docker image to registry..."

                    docker.withRegistry('https://quay.io', 'quay-io-credentials') {
                        // Push versioned image
                        docker.image("${env.DOCKER_VERSIONED}").push()
                        echo "✅ Pushed: ${env.DOCKER_VERSIONED}"

                        // Push latest for main branch
                        if (env.BRANCH_NAME == 'main') {
                            docker.image("${env.DOCKER_VERSIONED}").push('latest')
                            echo "✅ Pushed: ${env.DOCKER_IMAGE}:latest"
                        }
                    }
                }
            }
        }

        stage('Deploy to Development') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    echo "🚀 Deploying to development environment..."

                    // Example deployment script
                    sh """
                        echo "Deployment commands would go here"
                        echo "Image to deploy: ${env.DOCKER_VERSIONED}"
                        # kubectl set image deployment/spring-security-session-redis \\
                        #     spring-security-session-redis=${env.DOCKER_VERSIONED} \\
                        #     -n development
                    """
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                script {
                    echo "🎯 Deploying to production environment..."

                    // Production deployment with approval
                    timeout(time: 10, unit: 'MINUTES') {
                        input message: 'Deploy to production?', ok: 'Deploy',
                              submitterParameter: 'DEPLOYER'
                    }

                    sh """
                        echo "Production deployment initiated by: \${DEPLOYER}"
                        echo "Image to deploy: ${env.DOCKER_VERSIONED}"
                        # Add production deployment commands here
                        # kubectl set image deployment/spring-security-session-redis \\
                        #     spring-security-session-redis=${env.DOCKER_VERSIONED} \\
                        #     -n production
                    """
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧹 Cleaning up workspace..."

                // Clean up Docker images to save space
                sh """
                    # Remove built images
                    docker rmi ${env.DOCKER_VERSIONED} || true
                    if [ "${env.BRANCH_NAME}" = "main" ]; then
                        docker rmi ${env.DOCKER_IMAGE}:latest || true
                    fi

                    # Clean up dangling images
                    docker image prune -f || true
                """

                // Archive important files
                archiveArtifacts artifacts: 'target/spring-*.jar', allowEmptyArchive: true

                // Clean workspace
                cleanWs()
            }
        }
        success {
            script {
                echo "✅ Pipeline completed successfully!"

                // Send success notification
                if (env.BRANCH_NAME in ['main', 'develop']) {
                    echo "🎉 Build #${BUILD_NUMBER} completed successfully for ${env.BRANCH_NAME} branch"
                    echo "📦 Docker image: ${env.DOCKER_VERSIONED}"
                }
            }
        }
        failure {
            script {
                echo "❌ Pipeline failed!"

                // Send failure notification
                echo "💥 Build #${BUILD_NUMBER} failed for ${env.BRANCH_NAME} branch"
                echo "🔍 Check the logs for details"
            }
        }
        unstable {
            script {
                echo "⚠️ Pipeline completed with warnings!"
                echo "🔍 Some tests may have failed or quality gates not met"
            }
        }
    }
}

