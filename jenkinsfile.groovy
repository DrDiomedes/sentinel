pipeline {
  agent any

  environment {
    TF_REPO = 'https://github.com/futurice/terraform-examples.git'
    POLICY_REPO = 'https://github.com/DrDiomedes/sentinel_policy.git'
  }

  stages {
    stage('Clonar código Terraform') {
      steps {
        dir('terraform') {
          git url: "${TF_REPO}", branch: 'main'
        }
      }
    }

    stage('Clonar políticas Sentinel') {
      steps {
        dir('policies') {
          git url: "${POLICY_REPO}", branch: 'main'
        }
      }
    }

    stage('Terraform Init') {
      steps {
        dir('terraform') {
          sh 'terraform init'
        }
      }
    }

    stage('Terraform Plan + JSON') {
      steps {
        dir('terraform') {
          sh 'terraform plan -out=tfplan.out'
          sh 'terraform show -json tfplan.out > tfplan.json'
        }
      }
    }

    stage('Instalar y ejecutar Sentinel') {
      steps {
        dir('terraform') {
          sh '''
            # Instalar Sentinel CLI (Linux x86_64)
            curl -o sentinel.zip https://releases.hashicorp.com/sentinel/0.30.1/sentinel_0.30.1_linux_amd64.zip
            unzip sentinel.zip
            chmod +x sentinel
            mv sentinel /usr/local/bin/

            # Verificamos instalación
            sentinel version

            # Ejecutamos la política
            sentinel apply -trace -config=../policies/sentinel.hcl ../policies/policy/main.sentinel tfplan.json
          '''
        }
      }
    }
  }

  post {
    success {
      echo '✅ El código Terraform cumple con las políticas Sentinel.'
    }
    failure {
      echo '❌ El código no cumple con las políticas Sentinel. Revisa el log.'
    }
  }
}
