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
          git url: "${TF_REPO}", branch: 'master'
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

    stage('Instalar Terraform') {
      steps {
        sh '''
          # Instalar Terraform en tiempo de ejecución (versión ajustable)
          echo "Descargando Terraform..."
          curl -O https://releases.hashicorp.com/terraform/1.8.5/terraform_1.8.5_linux_amd64.zip
    
          echo "Descomprimiendo..."
          unzip terraform_1.8.5_linux_amd64.zip
    
          echo "Moviendo binario a PATH local"
          mkdir -p $HOME/bin
          mv terraform $HOME/bin/
    
          # Añadir al PATH para el resto del pipeline
          export PATH=$HOME/bin:$PATH
          echo 'export PATH=$HOME/bin:$PATH' >> ~/.bashrc
    
          echo "Verificando versión de Terraform"
          terraform version
        '''
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
