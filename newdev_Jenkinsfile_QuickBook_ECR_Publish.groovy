node{
  try{
    properties([

      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        // choice(choices: ['dev', 'qa', 'uat', 'prod'], description: '', name: 'BuildProfile'),
        choice(choices: ['newdev'], description: '', name: 'BuildProfile'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-QuickBook-API.git', tagsFilter: '']
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
      dir('BidClips-QuickBook-API') {
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]
          PUBLISHTAG = TagName.split('/')[1]
          repoRegion = "us-east-1"
        }
        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "us-west-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-QuickBook-API.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "us-west-1"

        }
      }
    }

    stage('Building Docker Image'){
      dir('BidClips-QuickBook-API') {
        def BUILDENV = ""
        if(BuildProfile=="newdev"){
          BUILDENV="dev"
        } else {
          BUILDENV="prod"
        }
        sh """
        echo "Build Profile : ${BUILDENV}"
        sudo update-alternatives --set java /usr/lib/jvm/java-11-openjdk-11.0.11.0.9-1.amzn2.0.1.x86_64/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-11-openjdk-11.0.11.0.9-1.amzn2.0.1.x86_64/bin/javac
        ./gradlew bootJar -P${BUILDENV} jibDockerBuild
        sudo update-alternatives --set java /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.282.b08-1.amzn2.0.1.x86_64/jre/bin/java
        sudo update-alternatives --set javac /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.282.b08-1.amzn2.0.1.x86_64/bin/javac
        docker images | grep bidclipsquickbookapi
        """
      }
    }
    stage("Publishing ${PUBLISHTAG}"){
      sh """
export AWS_PROFILE=bidclips-eks
aws ecr get-login-password --region ${repoRegion} | docker login --username AWS --password-stdin 566570633830.dkr.ecr.${repoRegion}.amazonaws.com
docker tag bidclipsquickbookapi:latest 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
docker push 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
        """
      if(repoRegion == "us-east-1"){
        // us-east-1 means docker image built from tag; not branch
        // if so, push it to prod ecr as well, to avoid cross-tenant authorization pain
        sh """
export AWS_PROFILE=bidclips-prod-ecr
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 875588116685.dkr.ecr.us-east-1.amazonaws.com
docker tag bidclipsquickbookapi:latest 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
docker push 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
docker image rmi -f 875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
        """
      }
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclipsquickbookapi:latest
      docker image rmi -f 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-quickbook-api:${PUBLISHTAG}
      """
      dir('BidClips-QuickBook-API') {
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
