node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['prod'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Provider-Portal.git', tagsFilter: '']
      ])
    ])
    def DEPLOYTAG = ""
    def repoRegion = ""
    def dockerImageWithTag = ""
    def bootstrapper = [
      "dev": "18.140.71.163",
      "qa": "18.140.71.163",
      "uat": "18.140.71.163",
      "prod": "18.140.71.163"
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
        dir('BidClips-Web-Provider-Portal') {
          if (TagName.startsWith('branches')) {
            branch = TagName.split('/')[1]
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:bidclips/bidclips-web-provider-portal.git']]]
          }
          if (TagName.equals('trunk')) {
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:bidclips/bidclips-web-provider-portal.git']]]
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
        dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-service-station:${DEPLOYTAG}".replace(':','\\:')
      }
      else{
        dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-service-station:${DEPLOYTAG}".replace(':','\\:')
      }
      dir("BidClips-Infrastructure") {
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' servicestation.yaml
scp servicestation.yaml ec2-user@18.140.71.163:/home/ec2-user/servicestation.yaml
        """
      }
    }
    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@18.140.71.163 /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh servicestation.yaml
kubectl -f servicestation.yaml
rm servicestation.yaml
sleep 5;
kubectl -n app-stack get deploy | grep "service-station"
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
