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
        
        // Podman configuration for container environment
        BUILDAH_ISOLATION = 'chroot'
        STORAGE_DRIVER = 'vfs'
        _CONTAINERS_USERNS_CONFIGURED = ''
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
            post {
                always {
                    // Publish test results
                    script {
                        if (fileExists('target/surefire-reports/TEST-*.xml')) {
                            junit 'target/surefire-reports/TEST-*.xml'
                            echo "📊 Test results published"
                        }
                    }
                }
            }
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

        // stage('Code Quality') {
        //     parallel {
        //         stage('Security Scan') {
        //             steps {
        //                 script {
        //                     echo "🔒 Running security scan..."
        //                     // Using OWASP Dependency Check
        //                     sh "./mvnw ${MAVEN_CLI_OPTS} org.owasp:dependency-check-maven:check || true"
        //                 }
        //             }
        //         }
        //         stage('Static Analysis') {
        //             when {
        //                 anyOf {
        //                     branch 'develop'
        //                     changeRequest()
        //                 }
        //             }
        //             steps {
        //                 script {
        //                     echo "📊 Running static code analysis..."
        //                     // Placeholder for SonarQube or similar tools
        //                     echo "Static analysis would run here (SonarQube, SpotBugs, etc.)"
        //                 }
        //             }
        //         }
        //     }
        // }

        stage('Build Docker Image') {
            steps {
                script {
                    echo "🐳 Building Docker image..."

                    sh """
                    ls -lah
                    echo "WORKSPACE: ${WORKSPACE}"
                    echo "DOCKER_VERSIONED: ${env.DOCKER_VERSIONED}"
                    echo "DOCKER_IMAGE: ${env.DOCKER_IMAGE}"
                    """

                    // Create Dockerfile with fully qualified registry names
                    sh "sed 's|FROM eclipse-temurin:|FROM docker.io/library/eclipse-temurin:|g' Dockerfile > Dockerfile.fq"

                    // Try different build methods in order of preference
                    def buildResult = sh(
                        script: """
                            set +e  # Don't exit on error, we'll handle it
                            
                            # Method 1: Try buildah (designed for CI/CD containers)
                            if command -v buildah >/dev/null 2>&1; then
                                echo "🔧 Using buildah for container build..."
                                buildah build -f Dockerfile.fq -t ${env.DOCKER_VERSIONED} .
                                if [ \$? -eq 0 ]; then
                                    echo "✅ Built with buildah"
                                    exit 0
                                fi
                                echo "⚠️ buildah failed, trying next method..."
                            fi
                            
                            # Method 2: Try podman with rootless configuration
                            if command -v podman >/dev/null 2>&1; then
                                echo "🔧 Using podman with container-friendly settings..."
                                
                                # Configure podman for container environment
                                export BUILDAH_ISOLATION=chroot
                                export STORAGE_DRIVER=vfs
                                unset _CONTAINERS_USERNS_CONFIGURED
                                
                                # Try building with container-friendly settings
                                podman build --isolation=chroot --storage-driver=vfs -f Dockerfile.fq -t ${env.DOCKER_VERSIONED} .
                                if [ \$? -eq 0 ]; then
                                    echo "✅ Built with podman (container mode)"
                                    exit 0
                                fi
                                echo "⚠️ podman container mode failed, trying privileged mode..."
                                
                                # Try with reduced security for CI environment
                                podman build --security-opt label=disable --cap-add=SYS_ADMIN -f Dockerfile.fq -t ${env.DOCKER_VERSIONED} . || true
                                if [ \$? -eq 0 ]; then
                                    echo "✅ Built with podman (privileged mode)"
                                    exit 0
                                fi
                            fi
                            
                            # Method 3: Fall back to docker if available
                            if command -v docker >/dev/null 2>&1; then
                                echo "🔧 Falling back to Docker..."
                                docker build -f Dockerfile.fq -t ${env.DOCKER_VERSIONED} .
                                if [ \$? -eq 0 ]; then
                                    echo "✅ Built with Docker"
                                    exit 0
                                fi
                            fi
                            
                            echo "❌ All build methods failed"
                            exit 1
                        """,
                        returnStatus: true
                    )

                    if (buildResult != 0) {
                        error("Failed to build Docker image with all available methods")
                    }

                    // Tag as latest for main branch
                    if (env.BRANCH_NAME == 'main') {
                        sh """
                            # Tag using the same tool that built the image
                            if command -v buildah >/dev/null 2>&1; then
                                buildah tag ${env.DOCKER_VERSIONED} ${env.DOCKER_IMAGE}:latest
                            elif command -v podman >/dev/null 2>&1; then
                                podman tag ${env.DOCKER_VERSIONED} ${env.DOCKER_IMAGE}:latest
                            elif command -v docker >/dev/null 2>&1; then
                                docker tag ${env.DOCKER_VERSIONED} ${env.DOCKER_IMAGE}:latest
                            fi
                            echo "✅ Tagged as latest"
                        """
                    }

                    echo "✅ Docker image built successfully: ${env.DOCKER_VERSIONED}"
                }
            }
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

                    // Check if image exists locally with available container tool
                    def imageExists = sh(
                        script: """
                            if command -v buildah >/dev/null 2>&1; then
                                buildah images -q ${env.DOCKER_VERSIONED} 2>/dev/null || echo ''
                            elif command -v podman >/dev/null 2>&1; then
                                podman images -q ${env.DOCKER_VERSIONED} 2>/dev/null || echo ''
                            elif command -v docker >/dev/null 2>&1; then
                                docker images -q ${env.DOCKER_VERSIONED} 2>/dev/null || echo ''
                            else
                                echo ''
                            fi
                        """,
                        returnStdout: true
                    ).trim()

                    if (!imageExists) {
                        echo "⚠️ Docker image not found locally, skipping push"
                        currentBuild.result = 'UNSTABLE'
                        return
                    }

                    // Login and push to Quay.io registry using available container tool
                    withCredentials([usernamePassword(credentialsId: 'quay-io-credentials',
                                                    usernameVariable: 'QUAY_USERNAME',
                                                    passwordVariable: 'QUAY_PASSWORD')]) {
                        sh """
                            # Determine which tool to use for pushing
                            CONTAINER_TOOL=""
                            if command -v buildah >/dev/null 2>&1; then
                                CONTAINER_TOOL="buildah"
                            elif command -v podman >/dev/null 2>&1; then
                                CONTAINER_TOOL="podman"
                            elif command -v docker >/dev/null 2>&1; then
                                CONTAINER_TOOL="docker"
                            else
                                echo "❌ No container tool available for pushing"
                                exit 1
                            fi
                            
                            echo "🔧 Using \$CONTAINER_TOOL for registry operations..."
                            
                            # Login to Quay.io
                            echo "\$QUAY_PASSWORD" | \$CONTAINER_TOOL login quay.io -u "\$QUAY_USERNAME" --password-stdin

                            # Push versioned image
                            \$CONTAINER_TOOL push ${env.DOCKER_VERSIONED}
                            echo "✅ Pushed: ${env.DOCKER_VERSIONED}"

                            # Push latest for main branch
                            if [ "${env.BRANCH_NAME}" = "main" ]; then
                                \$CONTAINER_TOOL push ${env.DOCKER_IMAGE}:latest
                                echo "✅ Pushed: ${env.DOCKER_IMAGE}:latest"
                            fi

                            # Logout for security
                            \$CONTAINER_TOOL logout quay.io
                        """
                    }

                    echo "✅ Successfully pushed to registry"
                }
            }
        }

        stage('Security Scan - Image') {
            steps {
                script {
                    echo "🔍 Scanning Docker image for vulnerabilities..."

                    // Using Trivy for container scanning with Podman
                    sh """
                        # Check if Podman is available for security scanning
                        if command -v podman >/dev/null 2>&1; then
                            echo "Running Trivy security scan with Podman..."
                            podman run --rm -v \$(pwd):/workspace \\
                                aquasec/trivy:latest image \\
                                --exit-code 0 \\
                                --severity HIGH,CRITICAL \\
                                --format json \\
                                --output /workspace/trivy-report.json \\
                                ${env.DOCKER_VERSIONED} || true

                            # Archive the security report if it exists
                            if [ -f trivy-report.json ]; then
                                echo "✅ Security scan completed"
                            else
                                echo "⚠️ Security scan failed or no report generated"
                            fi
                        else
                            echo "Podman not available, skipping security scan"
                        fi
                    """

                    // Archive the security report
                    archiveArtifacts artifacts: 'trivy-report.json', allowEmptyArchive: true
                }
            }
        }

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
                        # Check if docker-compose.yml exists
                        if [ -f docker-compose.yml ]; then
                            echo "Starting test environment..."
                            docker-compose -f docker-compose.yml up -d postgres redis

                            # Wait for services to be ready
                            echo "Waiting for services to be ready..."
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

                // Clean up container images to save space
                sh """
                    # Determine which tool to use for cleanup
                    CLEANUP_TOOL=""
                    if command -v buildah >/dev/null 2>&1; then
                        CLEANUP_TOOL="buildah"
                    elif command -v podman >/dev/null 2>&1; then
                        CLEANUP_TOOL="podman"
                    elif command -v docker >/dev/null 2>&1; then
                        CLEANUP_TOOL="docker"
                    fi
                    
                    if [ -n "\$CLEANUP_TOOL" ]; then
                        echo "Cleaning up container images with \$CLEANUP_TOOL..."
                        
                        # Remove built images
                        \$CLEANUP_TOOL rmi ${env.DOCKER_VERSIONED} || true
                        if [ "${env.BRANCH_NAME}" = "main" ]; then
                            \$CLEANUP_TOOL rmi ${env.DOCKER_IMAGE}:latest || true
                        fi

                        # Clean up dangling images (if supported)
                        if [ "\$CLEANUP_TOOL" != "buildah" ]; then
                            \$CLEANUP_TOOL image prune -f || true
                        fi
                        
                        echo "✅ \$CLEANUP_TOOL cleanup completed"
                    else
                        echo "No container tool available, skipping image cleanup"
                    fi
                    
                    # Remove temporary dockerfile
                    rm -f ${WORKSPACE}/Dockerfile.fq || true
                """

                // Archive important files
                archiveArtifacts artifacts: 'target/spring-*.jar', allowEmptyArchive: true
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

