node{
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['dev', 'qa', 'uat', 'prod'], description: '', name: 'BuildProfile'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Gateway.git', tagsFilter: '']
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
      dir('BidClips-Web-Gateway') {
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Gateway.git']]]
          PUBLISHTAG = TagName.split('/')[1]
          repoRegion = "ap-southeast-1"
        }
        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Gateway.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Gateway.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"

        }
      }
    }

    stage('Building Docker Image'){
      dir('BidClips-Web-Gateway') {
        def BUILDENV = ""
        if(BuildProfile=="dev"){
          BUILDENV="dev"
        } else {
          BUILDENV="prod"
        }
        sh """
        echo "Build Profile : ${BUILDENV}"
        ./gradlew bootJar -P${BUILDENV} jibDockerBuild
        docker images | grep bidclips
        """
      }
    }
    stage("Publishing ${PUBLISHTAG}"){
      sh """
export AWS_PROFILE=bidclips-eks
aws ecr get-login-password --region ${repoRegion} | docker login --username AWS --password-stdin 566570633830.dkr.ecr.${repoRegion}.amazonaws.com
docker tag bidclips:latest 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
docker push 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
        """
//       if(repoRegion == "us-east-1"){
//         // us-east-1 means docker image built from tag; not branch
//         // if so, push it to prod ecr as well, to avoid cross-tenant authorization pain
//         sh """
// export AWS_PROFILE=bidclips-prod-ecr
// aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 875588116685.dkr.ecr.us-east-1.amazonaws.com
// docker tag bidclips:latest 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
// docker push 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
// docker image rmi -f 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
//         """
//       }
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclips:latest
      docker image rmi -f 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-gateway:${PUBLISHTAG}
      """
      dir('BidClips-Web-Gateway') {
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
