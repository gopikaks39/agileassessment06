pipeline {
 agent any
 tools { jdk 'JDK17'; maven 'Maven' }
 stages {
  stage('Checkout') { steps { checkout scm } }
  stage('Compile') { steps { sh 'mvn clean compile' } }
  stage('Unit Tests') { steps { sh 'mvn test' } post { always { junit 'target/surefire-reports/*.xml' } } }
  stage('Package') { steps { sh 'mvn package -DskipTests' } }
 }
 post { success { echo 'Pipeline completed successfully' } failure { echo 'Pipeline failed' } }
}
