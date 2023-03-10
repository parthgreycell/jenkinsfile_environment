node{
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
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Widget.git', tagsFilter: '']
      ])
    ])
    def PUBLISHTAG = ""
    def repoRegion = ""

    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      dir('BidClips-Web-Widget') {
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Widget.git']]]
          PUBLISHTAG = TagName.split('/')[1]
          repoRegion = "ap-southeast-1"
        }
        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Widget.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Widget.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
      }

      dir('BidClips-Infrastructure') {
        // Cloning Infra repo for configurations
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
      }
    }
    stage('Building Binaries'){
      dir('BidClips-Infrastructure') {
        sh """
        ls -alh common/nginx/nginx-widget.conf
        cp common/nginx/nginx-widget.conf ../BidClips-Web-Widget/nginx-widget.conf
        ls -alh docker/Dockerfile-Widget
        cp docker/Dockerfile-Widget ../BidClips-Web-Widget/Dockerfile-Widget
        """
      }

      dir('BidClips-Web-Widget') {
        sh """
        cd widget/
        npmrc bidclips-tk-read
        npm install
        CI=false npm run build
        ls -alh build/
        """
      }
    }

    stage('Building Docker Image'){
      dir('BidClips-Web-Widget') {
        sh """
        docker build --file=Dockerfile-Widget --tag=bidclips-web-widget:${PUBLISHTAG} .
        """
      }
    }
    stage("Publishing ${PUBLISHTAG}"){
      sh """
export AWS_PROFILE=bidclips-eks
aws ecr get-login-password --region ${repoRegion} | docker login --username AWS --password-stdin 566570633830.dkr.ecr.${repoRegion}.amazonaws.com
docker tag bidclips-web-widget:${PUBLISHTAG} 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
docker push 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
        """
//       if(repoRegion == "us-east-1"){
//         // us-east-1 means docker image built from tag; not branch
//         // if so, push it to prod ecr as well, to avoid cross-tenant authorization pain
//         sh """
// export AWS_PROFILE=bidclips-prod-ecr
// aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 875588116685.dkr.ecr.us-east-1.amazonaws.com
// docker tag bidclips-web-widget:${PUBLISHTAG} 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
// docker push 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
// docker image rmi -f 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
//         """
//       }
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclips-web-widget:${PUBLISHTAG}
      docker image rmi -f 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-widget:${PUBLISHTAG}
      """
      dir('BidClips-Infrastructure') {
        deleteDir()
      }
      dir('BidClips-Web-Widget') {
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
