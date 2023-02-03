node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['dev', 'qa', 'uat', 'prod'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-API-Restheart.git', tagsFilter: '']
      ])
    ])
    def DEPLOYTAG = ""
    def dockerImageWithTag = ""
    def repoRegion = [
      "dev": "ap-southeast-1", /* "ap-south-1", */
      "qa": "ap-southeast-1",
      "uat": "ap-southeast-1",
      "prod": "ap-southeast-1"
    ]
    def bootstrapper = [
      "dev": "18.141.143.199",
      "qa": "18.141.143.199",
      "uat": "18.141.143.199",
      "prod": "18.141.143.199"
    ]
    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      if (TagName.startsWith('tags')) {
        DEPLOYTAG = TagName.split('/')[1]
        repoRegion = "ap-southeast-1"
      }
      else{
        dir('BidClips-API-Restheart') {
          if (TagName.startsWith('branches')) {
            branch = TagName.split('/')[1]
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-API-Restheart.git']]]
          }
          if (TagName.equals('trunk')) {
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-API-Restheart.git']]]
          }
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
          deleteDir()
        }
      }

      if(DeployEnv == "prod"){
        dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-api-restheart:${DEPLOYTAG}".replace(':','\\:')
      }
      else{
        dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-api-restheart:${DEPLOYTAG}".replace(':','\\:')
      }
      dir("BidClips-Infrastructure") {
        // checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        }
        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        }

        def mongo_uri_id = ''
        def awssqs_accesskey_id = ''
        def awssqs_secretKey_id = ''
        def awssqs_region_id = ''
        def awssqs_queue_id = ''
        def awssqs_v2_accesskey_id = ''
        def awssqs_v2_secretKey_id = ''
        def awssqs_v2_region_id = ''
        def awssqs_v2_queue_id = ''
        def bidclips_idm_id = ''

        if (DeployEnv == 'dev') {
          mongo_uri_id = "4de895fa-0e94-4f86-bb8d-99a9d601e1f1"
          awssqs_accesskey_id = "87297187-8a00-42e3-b09a-132ddf479129"
          awssqs_secretKey_id = "ceebc20f-3712-42b2-bc8d-c8373ede91df"
          awssqs_region_id = "e3262ff8-4243-42fc-bede-36f6ca321c7d"
          awssqs_queue_id = "6d1d0212-2539-41c1-b423-3a88a5453773"
          awssqs_v2_accesskey_id = "b5a943af-1d2d-4eba-ba5d-1e50ebf904bf"
          awssqs_v2_secretKey_id = "ad8eb5ef-b74f-460f-8e03-2b9c47cfc514"
          awssqs_v2_region_id = "ddea3d91-46ce-49f8-aaf1-e4a13ce4db9f"
          awssqs_v2_queue_id = "f50a0509-b2a0-4a36-8435-b09ec459bd75"
          bidclips_idm_id = "565a8c04-c6b0-43a5-a2a5-85dcc64c8272"
        }

        
        withCredentials([
              string(credentialsId: mongo_uri_id, variable: 'real_mongo_uri_id'),
              string(credentialsId: awssqs_accesskey_id, variable: 'real_awssqs_accesskey_id'),
              string(credentialsId: awssqs_secretKey_id, variable: 'real_awssqs_secretKey_id'),
              string(credentialsId: awssqs_region_id, variable: 'real_awssqs_region_id'),
              string(credentialsId: awssqs_queue_id, variable: 'real_awssqs_queue_id'),
              string(credentialsId: awssqs_v2_accesskey_id, variable: 'real_awssqs_v2_accesskey_id'),
              string(credentialsId: awssqs_v2_secretKey_id, variable: 'real_awssqs_v2_secretKey_id'),
              string(credentialsId: awssqs_v2_region_id, variable: 'real_awssqs_v2_region_id'),
              string(credentialsId: awssqs_v2_queue_id, variable: 'real_awssqs_v2_queue_id'),
              string(credentialsId: bidclips_idm_id, variable: 'real_bidclips_idm_id')
            ]) {
              real_mongo_uri_id=real_mongo_uri_id.replace('&','\\&')
              sh """
ls -alh common/BidClips-API-Restheart/etc/
sed -i 's#REPLACEME_MONGODB_CONNECTION_URL#$real_mongo_uri_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#\\REPLACEME_MONGODB_CONNECTION_URL#\\&#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_ACCESS_KEY_ID#$real_awssqs_accesskey_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_SECRET_KEY#$real_awssqs_secretKey_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_REGION#$real_awssqs_region_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_QUEUE#$real_awssqs_queue_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_V2_ACCESS_KEY_ID#$real_awssqs_v2_accesskey_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_V2_SECRET_KEY#$real_awssqs_v2_secretKey_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_V2_REGION#$real_awssqs_v2_region_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_V2_QUEUE#$real_awssqs_v2_queue_id#g' common/BidClips-API-Restheart/etc/restheart.yml
sed -i 's#REPLACEME_IDM_SECRET#$real_bidclips_idm_id#g' common/BidClips-API-Restheart/etc/restheart.yml
cd common/BidClips-API-Restheart/
tar -czf restheart-conf.tar.gz etc/
scp restheart-conf.tar.gz appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/restheart-conf.tar.gz
rm restheart-conf.tar.gz
              """
        }
      }
      dir("BidClips-EKS-Manifest"){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' restheart.yaml
scp restheart.yaml appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/restheart.yaml
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt appuser@${bootstrapper.get(DeployEnv)} /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh restheart.*
tar -xzf restheart-conf.tar.gz
rm restheart-conf.tar.gz
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack delete configmap restheart-config
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack create configmap restheart-config --from-file=etc/
sleep 5;
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config apply -f restheart.yaml
rm restheart.*
sleep 5;
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack get deploy | grep restheart
exit
EOA
      """
    }

    stage("cleanup"){
      dir("BidClips-EKS-Manifest"){
        deleteDir()
      }
      dir("BidClips-API-Restheart"){
        deleteDir()
      }
      dir("BidClips-Infrastructure"){
        deleteDir()
      }
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
