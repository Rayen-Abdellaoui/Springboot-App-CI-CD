pipeline {
    agent any
    
    tools {
        jdk 'jdk17'
        maven 'maven3'
    }
 
    environment{
        GITHUB_CREDS = 'github-credentials'
        GITHUB_URL = 'https://github.com/Rayen-Abdellaoui/Springboot-App-CI-CD'
        GIT_EMAIL = 'abdellaouirayen219@gmail.com'
        GIT_NAME = 'Rayen-Abdellaoui'
        DEPLOYMENT_FILE = 'k8s-deployment/deployment.yaml'
        GITHUB_REPO = 'Rayen-Abdellaoui/Springboot-App-CI-CD'
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
        CLUSTER_NAME = 'docker-desktop'
        K8S_CREDS = 'k8s-cred'
        NAMESPACE = 'springboot-namespace'
        K8S_URL = 'https://kubernetes.docker.internal:6443'

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
                sh "trivy fs  --format table --exit-code 1 --severity CRITICAL,HIGH  . > trivyfs.html" // --exit-code 1 --severity CRITICAL,HIGH
                archiveArtifacts artifacts: 'trivyfs.html', fingerprint: true
            }
        }
        
        stage('Dependency Check') {
            steps {
                dependencyCheck additionalArguments: '--format HTML --failOnCVSS 4.0' , odcInstallation: 'DP_Check' // --failOnCVSS 4.0 : accept on low vulnerability severity 
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
                    def zapStatus = sh(
                        script: """
                             run --rm -u \$(id -u):\$(id -g) \
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
                        echo "ZAP reported WARNs but no FAILs. Continuing pipeline."
                    } else if (zapStatus != 0) {
                         error "❌ ZAP scan failed with exit code ${zapStatus}"
                    } else {
                        echo = "ZAP scan passed with no FAILs or WARNs."
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
        
        stage('Update image tag in deployment.yaml') {
          steps {
            script {
              sh """
                sed -i "s|image: ${DOCKER_IMAGE}:.*|image: ${DOCKER_IMAGE}:${IMAGE_TAG}|" k8s-deployment/deployment.yaml
              """
            }
          }
        }
        
        stage('Commit and push changes') {
          steps {
            withCredentials([usernamePassword(credentialsId: env.GITHUB_CREDS, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
              sh '''
                git config user.email "${GIT_EMAIL}"
                git config user.name "${GIT_NAME}"
                git add ${DEPLOYMENT_FILE}
                git commit -m "Update image tag to ${IMAGE_TAG}" || echo "No changes to commit"
                git push https://${GIT_NAME}:${GIT_PASS}@github.com/${GITHUB_REPO}.git ${GIT_BRANCH}
              '''
            }
          }
        }

        
        stage('Deploy to Kubernetes') {
            steps {
                script{
                    withKubeConfig(caCertificate: '', clusterName: env.CLUSTER_NAME, contextName: '', credentialsId: env.K8S_CREDS, namespace: env.NAMESPACE, restrictKubeConfigAccess: false, serverUrl: 'https://kubernetes.docker.internal:6443') {
                        sh 'kubectl apply -f k8s-deployment/deployment.yaml -f k8s-deployment/service.yaml'
                    }     
                }
            }
        }
        
        
        stage('Verify Deployment') {
            steps {
                script{
                    withKubeConfig(caCertificate: '', clusterName: env.CLUSTER_NAME, contextName: '', credentialsId: env.K8S_CREDS, namespace: env.NAMESPACE, restrictKubeConfigAccess: false, serverUrl: env.K8S_URL) {
                        sleep time: 20, unit: 'SECONDS'
                        sh 'kubectl get pods -n ${NAMESPACE}'
                        sh 'kubectl get svc -n ${NAMESPACE}'
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