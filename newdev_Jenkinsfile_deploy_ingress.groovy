node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: true),
      disableResume(),
      parameters([
        choice(choices: ['newdev'], description: '', name: 'DeployEnv')
      ])
    ])
    def REGION = ""
    def bootstrapper = [
      "newdev": "52.52.222.149"
    ]
    def DOMAIN = ""
    stage('Preparation'){
      dir('BidClips-Infrastructure'){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        def certificateARN_id = ''
        if (DeployEnv == 'newdev') {
          certificateARN_id = '62997356-2883-456e-87c5-b1168133094c'
          DOMAIN = "providers-next.bidclips.dev"
          REGION = "us-west-1"
        }
        if (DeployEnv == 'qa') {
          certificateARN_id = 'f1dd5be4-493f-446b-bcf3-c1af326dd0b6'
          DOMAIN = "bidclips.tech"
          REGION = "ap-south-1"
        }
        if (DeployEnv == 'uat') {
          certificateARN_id = '93ea70b5-3631-44e1-8886-497ac34769c2'
          DOMAIN = "bidclipsuat.com"
          REGION = "us-east-1"
        }
        if (DeployEnv == 'prod') {
          certificateARN_id = 'da444fda-8930-4ddf-be52-bf237f228bd7'
          DOMAIN = "bidclips.com"
          REGION = "us-east-1"
        }

        withCredentials([string(credentialsId: certificateARN_id, variable: 'certificateARN')]) {
          certificateARN=certificateARN.replace(':','\\:').replace('/','\\/')
          sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_CERTIFICATE_ARN#$certificateARN#g' ingress.yaml
sed -i 's#REPLACEME_DOMAIN_NAME#$DOMAIN#g' ingress.yaml
sed -i 's#REPLACEME_DEPLOYENV#$DeployEnv#g' ingress.yaml
cat ingress.yaml
scp ingress.yaml appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/ingress.yaml
          """
        }
      }
    }

    stage("Deploying"){
      sh """
ssh -tt appuser@${bootstrapper.get(DeployEnv)} /bin/bash << EOA
export AWS_DEFAULT_REGION="${REGION}"
ls -lh ingress.yaml
kubectl apply -f ingress.yaml
rm ingress.yaml
sleep 5;
kubectl -n app-stack describe ingress bidclips-ingress
exit
EOA
      """
    }
  }
  catch( exec ) {
    echo "FAILURE: ${exec}"
    currentBuild.result = 'FAILURE'
  }

  finally {
    echo 'FINALLY BLOCK!'
    cleanWs()
    if (currentBuild.result == 'UNSTABLE') {
      echo 'I am unstable :/'
    }
    else if (currentBuild.result == 'FAILURE'){
      echo 'FAILURE!'
    }
    else {
      echo 'One way or another, I have finished'
    }
  }
}
