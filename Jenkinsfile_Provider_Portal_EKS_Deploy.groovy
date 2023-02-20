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
        choice(choices: ['dev', 'qa', 'uat', 'prod'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Provider-Portal.git', tagsFilter: '']
      ])
    ])
    def DEPLOYTAG = ""
    def repoRegion = ""
    def dockerImageWithTag = ""
    def bootstrapper = [
      "dev": "3.0.102.120"
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
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Provider-Portal.git']]]
          }
          if (TagName.equals('trunk')) {
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Provider-Portal.git']]]
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
        dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-provider-portal:${DEPLOYTAG}".replace(':','\\:')
      }
      else{
        dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-provider-portal:${DEPLOYTAG}".replace(':','\\:')
      }
      dir("BidClips-Infrastructure") {
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        sh """
cd BidClips-EKS/Kubernetes/application-stack/
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#$dockerImageWithTag#g' provider-portal.yaml
scp provider-portal.yaml ec2-user@3.0.102.120:/home/ec2-user/provider-portal.yaml
        """
      }
    }
    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt ec2-user@3.0.102.120 /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh provider-portal.yaml
kubectl apply -f provider-portal.yaml
rm provider-portal.yaml
sleep 5;
kubectl -n app-stack get deploy | grep "provider-portal"
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
