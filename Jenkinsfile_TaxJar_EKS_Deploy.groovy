node("built-in"){
  try{
    properties([
      authorizationMatrix([
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:DeepChavhan',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:DrashtiVora',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:JagdishChauhan',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:NidhiPrajapati2410',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:Tanjil-ghanchi',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:birengreylab',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:haarshjani',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:jay-patel-gclabs',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:kshraval',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:rushitpadia',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:saurabh-greylabs',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:shubhampatelskp',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:urjit0204',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:BhaveshVaza',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:Lakhan22',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:NiraliPatel142',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:Parnasi',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:RK9260',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:dhrumit99',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:nikpithadiya',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:prajapativiral',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:savan01',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:siddiqa-pathan',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:urvashi-godumalani',
        'USER:hudson.model.Item.Build:kshraval',
        'USER:hudson.model.Item.Cancel:kshraval',
        'USER:hudson.model.Item.Read:DeepChavhan',
        'USER:hudson.model.Item.Read:DrashtiVora',
        'USER:hudson.model.Item.Read:JagdishChauhan',
        'USER:hudson.model.Item.Read:NidhiPrajapati2410',
        'USER:hudson.model.Item.Read:Tanjil-ghanchi',
        'USER:hudson.model.Item.Read:birengreylab',
        'USER:hudson.model.Item.Read:haarshjani',
        'USER:hudson.model.Item.Read:jay-patel-gclabs',
        'USER:hudson.model.Item.Read:kshraval',
        'USER:hudson.model.Item.Read:rushitpadia',
        'USER:hudson.model.Item.Read:saurabh-greylabs',
        'USER:hudson.model.Item.Read:shubhampatelskp',
        'USER:hudson.model.Item.Read:urjit0204',
        'USER:hudson.model.Item.Workspace:kshraval',
        'USER:hudson.model.Item.Read:BhaveshVaza',
        'USER:hudson.model.Item.Read:Lakhan22',
        'USER:hudson.model.Item.Read:NiraliPatel142',
        'USER:hudson.model.Item.Read:Parnasi',
        'USER:hudson.model.Item.Read:RK9260',
        'USER:hudson.model.Item.Read:dhrumit99',
        'USER:hudson.model.Item.Read:nikpithadiya',
        'USER:hudson.model.Item.Read:prajapativiral',
        'USER:hudson.model.Item.Read:savan01',
        'USER:hudson.model.Item.Read:siddiqa-pathan',
        'USER:hudson.model.Item.Read:urvashi-godumalani'
      ]),
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['newdev'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-TaxJar-API.git']
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
      else{
        dir('BidClips-TaxJar-API') {
          if (TagName.startsWith('tags')) {
            DEPLOYTAG = TagName.split('/')[1]
            repoRegion = "ap-southeast-1"
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-TaxJar-API.git']]]

          }
          if (TagName.startsWith('branches')) {
            repoRegion = "ap-southeast-1"
            branch = TagName.split('/')[1]
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-TaxJar-API.git']]]
              DEPLOYTAG = sh(
              script: 'echo $(git log -1 --pretty=%h)',
              returnStdout: true
            ).trim()
          }
          if (TagName.equals('trunk')) {
            repoRegion = "ap-southeast-1"
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-TaxJar-API.git']]]
            DEPLOYTAG = sh(
              script: 'echo $(git log -1 --pretty=%h)',
              returnStdout: true
            ).trim()
          }

          def taxjar_api_key = ""
          def taxjar_path = ""
          def mongo_uri = ""
          def restheart_url = ""
          def base64_secret = ""
          if(DeployEnv=="newdev"){
            taxjar_api_key = "9746ef40-e353-46e6-841f-c36309a99ec8"
            taxjar_path = "e8e70e6c-9dd2-434d-be12-ad74cad44f95"
            base64_secret = "7d577f14-79f4-49fd-80b6-eddc68b92a88"
            mongo_uri = "fa0513f2-3763-4c81-ac42-fb1945efbd2a"
            restheart_url = "7a3edf37-bb4a-4a95-9f08-02f662762e02"
          }
          // if(DeployEnv=="qa"){
          //   taxjar_api_key = "9746ef40-e353-46e6-841f-c36309a99ec8"
          //   taxjar_path = "e8e70e6c-9dd2-434d-be12-ad74cad44f95"
          //   base64_secret = "49fe8b37-ac9c-4723-8e88-8833fffc52fe"
          //   // above credentials are of dev env
          //   mongo_uri = "c30e77e1-8741-452f-ba58-f4d0724eb1c4"
          //   restheart_url = "d7c6eeed-22ae-49ca-ad39-a1f4a36ca096"
          // }
          // if(DeployEnv=="uat"){
          //   taxjar_api_key = "9746ef40-e353-46e6-841f-c36309a99ec8"
          //   taxjar_path = "e8e70e6c-9dd2-434d-be12-ad74cad44f95"
          //   mongo_uri = "2311be07-35c4-4296-ba5b-a11e637e1787"
          //   restheart_url = "3c287199-5a6f-4759-b4cb-43cbc0dd0a2c"
          //   base64_secret = "6852d2ad-9d2e-45ef-8df4-74a9e75db153"
          // }
          // if(DeployEnv=="prod"){
          //   taxjar_api_key = "9746ef40-e353-46e6-841f-c36309a99ec8"
          //   taxjar_path = "e8e70e6c-9dd2-434d-be12-ad74cad44f95"
          //   mongo_uri = "e1af3291-3e80-49c8-b5d0-77cd8cb6119c"
          //   restheart_url = "c0d52f8d-bf6e-418b-93e2-6d1048130843"
          //   base64_secret = "0e3deb42-4fc5-4d1b-b6bd-91019debae9e"
          // }
          withCredentials([
            string(credentialsId: taxjar_api_key, variable: 'var_taxjar_api_key'),
            string(credentialsId: taxjar_path, variable: 'var_taxjar_path'),
            string(credentialsId: mongo_uri, variable: 'var_mongo_uri'),
            string(credentialsId: restheart_url, variable: 'var_restheart_url'),
            string(credentialsId: base64_secret, variable: 'var_base64_secret')
            ]) {
              var_taxjar_path=var_taxjar_path.replace('/','\\/').replace(':','\\:')
              var_restheart_url=var_restheart_url.replace('/','\\/').replace(':','\\:')
              var_mongo_uri=var_mongo_uri.replace('&','\\&')
                sh """
ls src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_MONGODB_URI#$var_mongo_uri#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_BASE64_SECRET#$var_base64_secret#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_TAXJAR_API_KEY#$var_taxjar_api_key#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_TAXJAR_PATH#$var_taxjar_path#g' src/main/resources/config/application-prod.yml
sed -i 's#REPLACEME_RESTHEART_URL#$var_restheart_url#g' src/main/resources/config/application-prod.yml
mkdir taxjar-config
cp src/main/resources/config/application-prod.yml taxjar-config/
cp taxjar-config/application-prod.yml taxjar-config/application-dev.yml
tar -czf taxjar-config.tar.gz taxjar-config/
rm -rf taxjar-config/
scp taxjar-config.tar.gz ec2-user@3.0.102.120:/home/ec2-user/taxjar-config.tar.gz
rm taxjar-config.tar.gz
                """
            }
          deleteDir()
        }
      }
      dir('BidClips-Infrastructure'){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]

        if(DeployEnv == "prod"){
          dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-taxjar-api:${DEPLOYTAG}".replace(':','\\:')
        }
        else{
          dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-taxjar-api:${DEPLOYTAG}".replace(':','\\:')
        }
        sh """
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' BidClips-EKS/Kubernetes/application-stack/taxjar.yaml
scp BidClips-EKS/Kubernetes/application-stack/taxjar.yaml ec2-user@3.0.102.120:/home/ec2-user/taxjar.yaml
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@3.0.102.120 /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh taxjar*
tar -xzf taxjar-config.tar.gz
rm taxjar-config.tar.gz
kubectl -n app-stack delete configmap taxjar-config
kubectl -n app-stack create configmap taxjar-config --from-file=taxjar-config/
sleep 5;
rm -rf taxjar-config/
kubectl apply -f taxjar.yaml
rm taxjar*
sleep 5;
kubectl -n app-stack get deploy | grep taxjar
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
