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

    }

    stages {
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
                waitForQualityGate abortPipeline: false, credentialsId: env.SONAR_CREDS //change false to true 
            }
        }

        
        stage('Maven Build') {
            steps {
                    sh "mvn clean package"
            }
        }
        
        stage('Trivy File System Scan') {
            steps {
                sh "trivy fs . > trivyfs.txt"
                archiveArtifacts artifacts: 'trivyfs.txt', fingerprint: true
            }
        }
        
        stage('Dependency Check') {
            steps {
                dependencyCheck additionalArguments: '--format HTML' , odcInstallation: 'DP_Check'
                archiveArtifacts artifacts: 'dependency-check-report.html', fingerprint: true
            }
        }
        
        //stage('Start Spring Boot App (Temp for ZAP)') {
         //   steps {
            //    sh 'nohup java -jar target/*.jar &'
          //      sleep time: 10, unit: 'SECONDS'
          //  }
     //   }

      //  stage('OWASP ZAP DAST Scan') {
         //   steps {
            //    script {
               //     def targetURL = 'http://localhost:8080' // or change to exposed staging URL
                //    sh """
                     //    docker run --rm -u \$(id -u):\$(id -g) -v \$(pwd):/zap/wrk:rw zaproxy/zap-stable zap-baseline.py \
                             //   -t ${targetURL} \
                            //    -r zap-report.html \
                             //   -x zap-report.xml \
                              //  -J zap-report.json \
                              //  -m 3
                  //  """
               // }
           // }
      //  }
        
        stage('Build and Push Docker Image') {
            steps {
                script{
                        withDockerRegistry(credentialsId: env.NEXUS_CREDS , toolName: env.DOCKER_TOOL, url: "http://${REGISTRY_URL}") {
                        sh "docker build -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
                        sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                    }
                }
            }
        }
    }
}