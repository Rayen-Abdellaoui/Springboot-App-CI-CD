pipeline {
    agent any
    
    tools {
        jdk 'jdk17'
        maven 'maven3'
    }
 
    environment{
        GITHUB_CREDS = 'github-credentials'
        GITHUB_URL = 'https://github.com/Rayen-Abdellaoui/Springboot-App-CI-CD'
        SCANNER_HOME= tool 'sonar-scanner'
        //DOCKER_IMAGE = 'rayenabd/springbootapp'
        DOCKER_IMAGE = "${REGISTRY_URL}/${REPO_NAME}/${IMAGE_NAME}"
        REGISTRY_URL = 'localhost:8091'
        REPO_NAME = 'docker-registry'  
        IMAGE_NAME = 'springbootapp'
        IMAGE_TAG = "${new Date().format('yyyyMMddHHmmss')}"
        NEXUS_CREDS = 'nexus'        
        DOCKER_TOOL = 'docker'
        SONAR_CREDS = 'sonar-token'
        SONAR_TOOL = 'sonar'
        APP_URL = 'http://host.docker.internal:8080'

    }

    stages {
        
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Checkout ') {
            steps {
                git branch: 'main', credentialsId: env.GITHUB_CREDS, url: env.GITHUB_URL
            }
        }
        
        stage('Code Compile') {
            steps {
                    sh "mvn compile"
            }
        }
        
        stage('Run Test Cases') {
            steps {
                sh "mvn test"
            }
        }
        
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv(env.SONAR_TOOL) {
                    sh 'mvn sonar:sonar'
               }
           }
        }
        
        stage("Quality Gate") {
            steps {
                script {
                    def qualityGate = waitForQualityGate(abortPipeline: true, credentialsId: env.SONAR_CREDS)
                    if (qualityGate.status != 'OK') {
                        error "Pipeline aborted due to Quality Gate failure: ${qualityGate.status} - ${qualityGate.description ?: 'No additional details'}"
            }
        }
            }
        }

        
        stage('Maven Build') {
            steps {
                    sh "mvn clean package"
            }
        }
        
        stage('Trivy File System Scan') {
            steps {
                sh "trivy fs  --format table . > trivyfs.html" // --exit-code 1 --severity CRITICAL,HIGH
                archiveArtifacts artifacts: 'trivyfs.html', fingerprint: true
            }
        }
        
        stage('Dependency Check') {
            steps {
                dependencyCheck additionalArguments: '--format HTML' , odcInstallation: 'DP_Check'
                archiveArtifacts artifacts: 'dependency-check-report.html', fingerprint: true
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script{
                        withDockerRegistry(credentialsId: env.NEXUS_CREDS , toolName: env.DOCKER_TOOL, url: "http://${REGISTRY_URL}") {
                        sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                    }
                }
            }
        }
        
        stage('Start Spring Boot App') {
            steps {
                sh "docker run -d --rm --name Spring --network jenkins-net -p 8080:8080  ${DOCKER_IMAGE}:${IMAGE_TAG}"
                sleep time: 10, unit: 'SECONDS'
            }
        }

       stage('OWASP ZAP Scan') {
            steps {
                script {
                    def zapMessage = ""
                    def zapStatus = sh(
                        script: """
                            docker run --rm -u \$(id -u):\$(id -g) \
                                --network jenkins-net \
                                -v \$(pwd):/zap/wrk:rw \
                                zaproxy/zap-stable zap-baseline.py \
                                -t ${APP_URL} \
                                -r zap-report.html \
                                -x zap-report.xml \
                                -J zap-report.json \
                                -m 1
                        """,
                        returnStatus: true
                    )
        
                    if (zapStatus == 2) {
                        zapMessage = "ZAP reported WARNs but no FAILs. Continuing pipeline."
                        echo zapMessage  
                    } else if (zapStatus != 0) {
                        zapMessage = "❌ ZAP scan failed with exit code ${zapStatus}"
                        echo zapMessage
                    } else {
                        zapMessage = "ZAP scan passed with no FAILs or WARNs."
                        echo zapMessage
                    }
                }
            }
        }
        
        stage('Stop Spring Boot App') {
            steps {
                sh "docker stop Spring"
            }
        }
         stage('Trivy Docker Image scan') {
            steps {
                    sh "trivy image  --format table ${DOCKER_IMAGE}:${IMAGE_TAG} > trivyimage.html " // --exit-code 1 --severity CRITICAL,HIGH
                    archiveArtifacts artifacts: 'trivyimage.html', fingerprint: true
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script{
                        withDockerRegistry(credentialsId: env.NEXUS_CREDS , toolName: env.DOCKER_TOOL, url: "http://${REGISTRY_URL}") {
                            sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                    }
                }
            }
        }
        
    }
    
     post {
        always {
            script {
                def status = currentBuild.currentResult
                def message = currentBuild.description 
                emailext(
                    subject: "Jenkins Build: ${status} - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                    body: """
                             <p>Build URL: <a href="${env.BUILD_URL}">${env.BUILD_URL}</a></p>""",
                    to: 'rayenrayen1258@gmail.com',
                    mimeType: 'text/html',
                    attachmentsPattern: 'trivyfs.html,trivyimage.html,dependency-check-report.html,zap-report.html,zap-report.xml,zap-report.json'
                )
            }
        }
    }
}