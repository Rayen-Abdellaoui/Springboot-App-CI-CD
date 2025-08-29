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
        GIT_BRANCH = 'main'
        DEPLOYMENT_FILE =  'k8s-helm/values.yaml'
        GITHUB_REPO = 'Rayen-Abdellaoui/Springboot-App-CI-CD'
        SCANNER_HOME= tool 'sonar-scanner'
        REGISTRY_URL = '172.189.107.75:8091'
        REPO_NAME = 'docker-registry'  
        IMAGE_NAME = 'springbootapp'
        DOCKER_IMAGE = "${REGISTRY_URL}/${REPO_NAME}/${IMAGE_NAME}"
        IMAGE_TAG = "${new Date().format('yyyyMMddHHmmss')}"
        NEXUS_CREDS = 'nexus'        
        DOCKER_TOOL = 'docker'
        SONAR_CREDS = 'sonar-token'
        SONAR_TOOL = 'sonar'
        APP_URL = 'http://172.189.107.75:8085'
        KUBECONFIG_CREDENTIALS = 'kind-kubeconfig'
        CLUSTER_NAME = 'mycluster'
        K8S_CREDS = 'k8s-cred'
        NAMESPACE = 'springboot-namespace'
        K8S_URL =  'https://localhost:6443' 

    }

    stages {
        
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Git Checkout ') {
            steps {
                script {
                    try {
                        git branch: 'main', credentialsId: env.GITHUB_CREDS, url: env.GITHUB_URL
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }
        
        
        stage('Run Tests & Maven Build') {
            steps {
                 script {
                    try {
                        sh "mvn clean package surefire-report:report jacoco:report"
                        archiveArtifacts 'target/reports/surefire.html'
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }
        
        stage('Security Scans'){
            parallel{
                stage('SonarQube Analysis') {
                    steps {
                        script {
                            try {
                                withSonarQubeEnv(env.SONAR_TOOL) {
                                    sh 'mvn sonar:sonar'
                                }
                            } catch (err) {
                                failedStage = env.STAGE_NAME
                                error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                            }
                        }
                    }
                    post{
                        always{
                            script {
                                try {
                                    def qualityGate = waitForQualityGate(abortPipeline: true, credentialsId: env.SONAR_CREDS)
                                    if (qualityGate.status != 'OK') {
                                        error "Pipeline aborted due to Quality Gate failure: ${qualityGate.status} - ${qualityGate.description ?: 'No additional details'}"
                                    }
                                } catch (err) {
                                    failedStage = env.STAGE_NAME
                                    error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                                }
                            }
                        }
                    }
                }
                
                stage('Trivy File System Scan') {
                    steps {
                        script {
                            try {
                                sh "trivy fs --format table . > trivyfs.html" // --exit-code 1 --scanners vuln,secret,config --severity CRITICAL,HIGH,MEDIUM
                                archiveArtifacts artifacts: 'trivyfs.html', fingerprint: true
                            } catch (err) {
                                failedStage = env.STAGE_NAME
                                error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                            }
                        }
                    }
                }
                
                stage('Dependency Check') {
                    steps {
                        script {
                            try {
                                dependencyCheck additionalArguments: '--noupdate --format XML --out dependency-check-report --failOnCVSS 0.0', odcInstallation: 'DP_Check'
                                archiveArtifacts artifacts: 'dependency-check-report/dependency-check-report.xml', fingerprint: true
                
                                //Check if vulnerabilities exist by parsing the report
                                def report = readFile('dependency-check-report/dependency-check-report.xml')
                                if (report.contains('<severity>')) {
                                    error "Dependency-Check found vulnerabilities."
                                }
                            } catch (err) {
                                failedStage = env.STAGE_NAME
                                error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                            }
                        }
                    }
                    post {
                        always {
                            dependencyCheckPublisher pattern: 'dependency-check-report/dependency-check-report.xml', stopBuild: true
                        }
                    }
                }
            }
        }
        
        
        stage('Build Docker Image') {
            steps {
                script {
                    try {
                        withDockerRegistry(
                            credentialsId: env.NEXUS_CREDS,
                            toolName: env.DOCKER_TOOL,
                            url: "http://${REGISTRY_URL}"
                        ) {
                            sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                        }
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }


        
        stage('Trivy Docker Image scan') {
            steps {
                script {
                        try {
                            sh "trivy image  --format table  ${DOCKER_IMAGE}:${IMAGE_TAG} > trivyimage.html " // --exit-code 1 --severity CRITICAL,HIGH
                            archiveArtifacts artifacts: 'trivyimage.html', fingerprint: true
                        } catch (err) {
                            failedStage = env.STAGE_NAME
                            error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                        }
                }
            }
        }
        
        
        stage('Start Spring Boot App') {
            steps {
                script {
                        try {
                            sh "docker run -d --rm --name Spring -p 8085:8082  ${DOCKER_IMAGE}:${IMAGE_TAG}"
                            sleep time: 10, unit: 'SECONDS'
                        } catch (err) {
                            failedStage = env.STAGE_NAME
                            error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                        }
                }
            }
        }
        

        stage('OWASP ZAP Scan') {
            steps {
                script {
                    try {
                        sh '''
                        docker run --rm \
                          -u root \
                          -v "${WORKSPACE}:/zap/wrk:rw" \
                          zaproxy/zap-stable zap-baseline.py \
                          -t https://www.siratify.com \
                          -r zap-report.html \
                          -x zap-report.xml \
                          -J zap-report.json \
                          -d \
                          -I \
                          -l WARN \
                          -m 1 \
                          -z "-daemon"
                          '''
                        archiveArtifacts artifacts: 'zap-report.*', fingerprint: true
                        //Parse JSON report and fail on Medium/High alerts
                        def zap = readJSON file: 'zap-report.json'
                        def alerts = zap.site[0].alerts.findAll { it.riskcode.toInteger() >= 1 } 
        
                        if (alerts.size() > 0) {
                            echo "Found ${alerts.size()} Low/Medium/High vulnerabilities:"
                            alerts.each { a ->
                                echo " - ${a.alert} [${a.riskdesc}] at ${a.instances[0]?.uri}"
                            }
                            error "Failing pipeline due to ZAP vulnerabilities."
                        } else {
                            echo " No Low/Medium/High vulnerabilities found."
                        }

                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "${err}"
                    }
                }
            }
        }
        
        stage('Stop Spring Boot App') {
            steps {
                script {
                        try {
                            sh "docker stop Spring"
                        } catch (err) {
                            failedStage = env.STAGE_NAME
                            error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                        }
                }
                
            }
        }
        

        
        stage('Push Docker Image') {
            steps {
                script {
                    try {
                        withDockerRegistry(credentialsId: env.NEXUS_CREDS , toolName: env.DOCKER_TOOL, url: "http://${REGISTRY_URL}") {
                            sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                        }
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }
        
        stage('Update image tag in deployment.yaml') {
          steps {
              script {
                    try {
                        sh """
                            sed -i "s|tag: .*|tag: \\"${IMAGE_TAG}\\"|" k8s-helm/values.yaml
                          """
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
            }
          }
        }
        
        stage('Commit and push changes') {
          steps {
              script {
                    try {
                         withCredentials([usernamePassword(credentialsId: env.GITHUB_CREDS, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_PASS')]) {
                              sh '''
                                git config user.email "${GIT_EMAIL}"
                                git config user.name "${GIT_NAME}"
                                git add ${DEPLOYMENT_FILE}
                                git commit -m "Update image tag to ${IMAGE_TAG}" || echo "No changes to commit"
                                git push https://${GIT_NAME}:${GIT_PASS}@github.com/${GITHUB_REPO}.git ${GIT_BRANCH}
                              '''
                        }
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
            }
          }
        }
        
        stage('Test Kubernetes') {
            steps {
                script {
                    try {
                        withKubeConfig(caCertificate: '', clusterName: env.CLUSTER_NAME, contextName: 'kind-mycluster', credentialsId: env.K8S_CREDS, namespace: env.NAMESPACE, restrictKubeConfigAccess: false, serverUrl: env.K8S_URL) {
                            sh 'kubectl get pods -n springboot-namespace '
                        }
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    try {
                            withKubeConfig(caCertificate: '', clusterName: env.CLUSTER_NAME, contextName: 'kind-mycluster', credentialsId: env.K8S_CREDS, namespace: env.NAMESPACE, restrictKubeConfigAccess: false, serverUrl: env.K8S_URL) {
                                sh '''helm upgrade --install springboot ./k8s-helm -n springboot-namespace '''
                            }
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }

        
        
        stage('Verify Deployment') {
            steps {
                script {
                    try {
                        withKubeConfig(caCertificate: '', clusterName: env.CLUSTER_NAME, contextName: 'kind-mycluster', credentialsId: env.K8S_CREDS, namespace: env.NAMESPACE, restrictKubeConfigAccess: false, serverUrl: env.K8S_URL) {
                            sleep time: 20, unit: 'SECONDS'
                            sh "kubectl get pods -n ${NAMESPACE} "
                            sh "kubectl get svc -n ${NAMESPACE} "
                        }  
                    } catch (err) {
                        failedStage = env.STAGE_NAME
                        error "Stage '${env.STAGE_NAME}' failed: ${err.message}"
                    }
                }
            }
        }
        
    }
    
    post {
        always {
            script {
                def status = currentBuild.currentResult
                def buildUrl = env.BUILD_URL
                def buildNumber = env.BUILD_NUMBER
                def jobName = env.JOB_NAME
                def subject = "Jenkins Build: ${status} - ${jobName} #${buildNumber}"
                def htmlReport = readFile('target/reports/surefire.html')
                def body = """
                    <p><strong>Build Result:</strong> ${status}</p>
                    <p><strong>Job:</strong> ${jobName}</p>
                    <p><strong>Build Number:</strong> #${buildNumber}</p>
                    <p><strong>Build URL:</strong> <a href="${buildUrl}">${buildUrl}</a></p>
                """
    
                if (status == "FAILURE") {
                    body += """
                        <p><strong>Failed Stage:</strong> ${failedStage}</p>
                    """
                }
                body += """
                    <hr>
                    ${htmlReport}
                  """
    
                emailext(
                    subject: subject,
                    body: body,
                    to: 'rayenrayen1258@gmail.com',
                    mimeType: 'text/html',
                    attachmentsPattern: 'trivyfs.html,trivyimage.html,dependency-check-report.html,zap-report.html,zap-report.xml,zap-report.json'
                )
            }
        }
    }
}
