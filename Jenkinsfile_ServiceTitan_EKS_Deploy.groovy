node("built-in"){
  try{
    properties([
      authorizationMatrix([
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:langyufei',
        'USER:hudson.model.Item.Read:langyufei'
      ]),
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['newdev','qa','uat','prod'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-ServiceTitan-API.git']
      ])
    ])
    def DEPLOYTAG = ""
    def repoRegion = ""
    def bootstrapper = [
      "newdev": "3.0.102.120"
    ]
    def DOMAIN = ""
    def dockerImageWithTag = ""
    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      dir('BidClips-ServiceTitan-API') {
        if (TagName.startsWith('tags')) {
          DEPLOYTAG = TagName.split('/')[1]
          repoRegion = "ap-southeast-1"
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-ServiceTitan-API.git']]]

        }
        if (TagName.startsWith('branches')) {
          branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-ServiceTitan-API.git']]]
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-ServiceTitan-API.git']]]
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }

        def mongodb_connection_url = ""
        def base64_secret = ""
        def servicetitan_authentication_url = ""
        
        if(DeployEnv=="newdev"){
          // env creds here
          mongodb_connection_url = "fa0513f2-3763-4c81-ac42-fb1945efbd2a"
          base64_secret = "7d577f14-79f4-49fd-80b6-eddc68b92a88"
          servicetitan_authentication_url = "a71fec99-364f-4326-8fe5-801a3ce7fb46"
        }
        withCredentials([
          string(credentialsId: mongodb_connection_url, variable: 'var_mongodb_connection_url'),
          string(credentialsId: base64_secret, variable: 'var_base64_secret'),
          string(credentialsId: servicetitan_authentication_url, variable: 'var_servicetitan_authentication_url')
        ]) {
          // substitution happens here
          var_mongodb_connection_url=var_mongodb_connection_url.replace('&','\\&')
          // var_servicetitan_authentication_url=var_servicetitan_authentication_url.replace('/','\\/').replace(':','\\:')
          sh """
ls src/main/resources/config/application-parametrised.yml
mv src/main/resources/config/application-parametrised.yml src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_MONGODB_CONNECTION_URL#$var_mongodb_connection_url#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_BASE64_SECRET#$var_base64_secret#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_SERVICETITAN_AUTHENTICATION_URL#$var_servicetitan_authentication_url#g' src/main/resources/config/application-prod.yml
mkdir servicetitan-config
cp src/main/resources/config/application-prod.yml servicetitan-config/
cp servicetitan-config/application-prod.yml servicetitan-config/application-dev.yml
tar -czf servicetitan-config.tar.gz servicetitan-config/
rm -rf servicetitan-config/
scp servicetitan-config.tar.gz ec2-user@3.0.102.120:/home/ec2-user/servicetitan-config.tar.gz
rm servicetitan-config.tar.gz
          """
        }
        deleteDir()
      }
      dir('BidClips-Infrastructure'){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]

        if(DeployEnv == "prod"){
          dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-servicetitan-api:${DEPLOYTAG}".replace(':','\\:')
        }
        else{
          dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-servicetitan-api:${DEPLOYTAG}".replace(':','\\:')
        }
        sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' servicetitan.yaml
scp servicetitan.yaml ec2-user@3.0.102.120:/home/ec2-user/servicetitan.yaml
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@3.0.102.120 /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh servicetitan*
tar -xzf /home/ec2-user/servicetitan-config.tar.gz
rm servicetitan-config.tar.gz
kubectl -n app-stack delete configmap servicetitan-config
kubectl -n app-stack create configmap servicetitan-config --from-file=servicetitan-config/
sleep 5;
rm -rf servicetitan-config/
kubectl apply -f servicetitan.yaml
rm servicetitan*
sleep 5;
kubectl -n app-stack get deploy | grep servicetitan
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
