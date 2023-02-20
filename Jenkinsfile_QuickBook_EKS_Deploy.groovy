node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['dev'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-QuickBook-API.git']
      ])
    ])
    def DEPLOYTAG = ""
    def repoRegion = ""
    def bootstrapper = [
      "dev": "3.0.102.120"
    ]
    def DOMAIN = ""
    def dockerImageWithTag = ""
    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      dir('BidClips-QuickBook-API') {
        if (TagName.startsWith('tags')) {
          DEPLOYTAG = TagName.split('/')[1]
          repoRegion = "ap-southeast-1"
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]

        }
        if (TagName.startsWith('branches')) {
          branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }

        def mongodb_connection_url = ""
        def base64_secret = ""
        def restheart_url = ""
        def redirect_api = ""
        def quickbook_oauth2appclientid = ""
        def quickbook_oauth2appclientsecret = ""
        def quickbook_oauth2appredirecturi = ""
        def quickbook_intuitaccountingapihost = ""

        if(DeployEnv=="dev"){
          // env creds here
          mongodb_connection_url = "fa0513f2-3763-4c81-ac42-fb1945efbd2a"
          base64_secret = "7d577f14-79f4-49fd-80b6-eddc68b92a88"
          restheart_url = "7a3edf37-bb4a-4a95-9f08-02f662762e02"
          redirect_api = "105a6f07-5b89-4e06-9cc1-36d0d52192eb"
          quickbook_oauth2appclientid = "f83c08d3-cfaf-471b-8695-a4d227ad8004"
          quickbook_oauth2appclientsecret = "b89ca131-5008-433d-b54b-a9ebedb4f437"
          quickbook_oauth2appredirecturi = "6e18d9ca-674d-4dc4-bb36-d8e0de707594"
          quickbook_intuitaccountingapihost = "25eeb115-3c78-4f05-9fab-b70dbf30e85f"
        }
        withCredentials([
          string(credentialsId: mongodb_connection_url, variable: 'var_mongodb_connection_url'),
          string(credentialsId: base64_secret, variable: 'var_base64_secret'),
          string(credentialsId: restheart_url, variable: 'var_restheart_url'),
          string(credentialsId: redirect_api, variable: 'var_redirect_api'),
          string(credentialsId: quickbook_oauth2appclientid, variable: 'var_quickbook_oauth2appclientid'),
          string(credentialsId: quickbook_oauth2appclientsecret, variable: 'var_quickbook_oauth2appclientsecret'),
          string(credentialsId: quickbook_oauth2appredirecturi, variable: 'var_quickbook_oauth2appredirecturi'),
          string(credentialsId: quickbook_intuitaccountingapihost, variable: 'var_quickbook_intuitaccountingapihost')
        ]) {
          // substitution happens here
          var_mongodb_connection_url=var_mongodb_connection_url.replace('&','\\&')
          var_restheart_url=var_restheart_url.replace('/','\\/').replace(':','\\:')
          var_redirect_api=var_redirect_api.replace('/','\\/').replace(':','\\:')
          var_quickbook_oauth2appredirecturi=var_quickbook_oauth2appredirecturi.replace('/','\\/').replace(':','\\:')
          var_quickbook_intuitaccountingapihost=var_quickbook_intuitaccountingapihost.replace('/','\\/').replace(':','\\:')
          sh """
ls src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_MONGODB_CONNECTION_URL#$var_mongodb_connection_url#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_BASE64_SECRET#$var_base64_secret#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_RESTHEART_URL#$var_restheart_url#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_REDIRECT_API#$var_redirect_api#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_QUICKBOOK_OAUTH2APPCLIENTID#$var_quickbook_oauth2appclientid#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_QUICKBOOK_OAUTH2APPCLIENTSECRET#$var_quickbook_oauth2appclientsecret#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_QUICKBOOK_OAUTH2APPREDIRECTURI#$var_quickbook_oauth2appredirecturi#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_QUICKBOOK_INTUITACCOUNTINGAPIHOST#$var_quickbook_intuitaccountingapihost#g' src/main/resources/config/application-prod.yml
mkdir quickbook-config
cp src/main/resources/config/application-prod.yml quickbook-config/
cp quickbook-config/application-prod.yml quickbook-config/application-dev.yml
tar -czf quickbook-config.tar.gz quickbook-config/
rm -rf quickbook-config/
scp quickbook-config.tar.gz ec2-user@3.0.102.120:/home/ec2-user/quickbook-config.tar.gz
rm quickbook-config.tar.gz
          """
        }
        deleteDir()
      }
      dir('BidClips-Infrastructure'){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]

        if(DeployEnv == "prod"){
          dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-quickbook-api:${DEPLOYTAG}".replace(':','\\:')
        }
        else{
          dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-quickbook-api:${DEPLOYTAG}".replace(':','\\:')
        }
        sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' quickbook.yaml
scp quickbook.yaml ec2-user@3.0.102.120:/home/ec2-user/quickbook.yaml
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@3.0.102.120 /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh quickbook*
tar -xzf /home/ec2-user/quickbook-config.tar.gz
rm quickbook-config.tar.gz
kubectl -n app-stack delete configmap quickbook-config
kubectl -n app-stack create configmap quickbook-config --from-file=quickbook-config/
sleep 5;
rm -rf quickbook-config/
kubectl apply -f quickbook.yaml
rm quickbook*
sleep 5;
kubectl -n app-stack get deploy | grep quickbook
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
