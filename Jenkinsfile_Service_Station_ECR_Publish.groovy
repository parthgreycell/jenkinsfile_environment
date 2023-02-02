node("built-in"){
  try{
    properties([
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', defaultValue: '', maxTags: '', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Provider-Portal.git', tagsFilter: '']
      ])
    ])
    def PUBLISHTAG = ""
    def repoRegion = ""
    def GITHASH = ""

    stage('Preparation'){
      if (''.equals(TagName)){
        echo "[FAILURE] TagName parameter is required - Failed to build, value provided : ${TagName}"
        currentBuild.result = 'FAILURE'
        throw new RuntimeException("required parameter missing : ${TagName}");
      }
      dir('BidClips-Web-Provider-Portal') {
        if (TagName.startsWith('tags')) {
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com/BidClips/BidClips-Web-Provider-Portal.git']]]
          PUBLISHTAG = TagName.split('/')[1]
          repoRegion = "ap-southeast-1"
        }
        if (TagName.startsWith('branches')) {
          def branch = TagName.split('/')[1]
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com/BidClips/BidClips-Web-Provider-Portal.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        if (TagName.equals('trunk')) {
          TagName = 'branches/master'
          checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com/BidClips/BidClips-Web-Provider-Portal.git']]]
          PUBLISHTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
        }
        GITHASH = sh(
          script: 'echo $(git log -1 --pretty=%h)',
          returnStdout: true
        ).trim()

        sh """
npmrc bidclips-tk-read
npm install
        """
      }

      dir('BidClips-Infrastructure') {
        // Cloning Infra repo for configurations
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: "master"]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com/BidClips/BidClips-Web-Provider-Portal.git']]]
      }
    }

//     stage("Running test cases") {
//       dir('BidClips-Web-Provider-Portal') {
//         sh """
// npm run build-css
// CI=true npm run test:ci
//         """
//         // junit 'test/**/*.xml'
//       }
//     }

    stage('Building Binaries'){
      dir('BidClips-Infrastructure') {
        sh """
ls -alh common/nginx/nginx.conf
cp common/nginx/nginx.conf ../BidClips-Web-Provider-Portal/nginx.conf
ls -alh docker/Dockerfile-Provider-Portal
cp docker/Dockerfile-Provider-Portal ../BidClips-Web-Provider-Portal/Dockerfile-Provider-Portal
        """
      }

      dir('BidClips-Web-Provider-Portal') {
        sh """
GENERATE_SOURCEMAP=false CI=false npm run build
ls -alh build/
if [ -f "build/gitHashVersion.js" ]; then
sed -i 's#REPLACEME_VERSION#${GITHASH}#g' build/gitHashVersion.js
cat build/gitHashVersion.js
fi
        """
      }
    }

    stage('Building Docker Image'){
      dir('BidClips-Web-Provider-Portal') {
        sh """
        docker build --file=Dockerfile-Provider-Portal --tag=bidclips-web-provider-portal:${PUBLISHTAG} .
        """
      }
    }
    stage("Publishing ${PUBLISHTAG}"){
      sh """
export AWS_PROFILE=bidclips-eks
aws ecr get-login-password --region ${repoRegion} | docker login --username AWS --password-stdin 566570633830.dkr.ecr.${repoRegion}.amazonaws.com
docker tag bidclips-web-provider-portal:${PUBLISHTAG} 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-service-station:${PUBLISHTAG}
docker push 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-service-station:${PUBLISHTAG}
        """
    }
    stage('Cleanup'){
      sh """
      docker image rmi -f bidclips-web-provider-portal:${PUBLISHTAG}
      """
      // docker image rmi -f 566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-service-station:${PUBLISHTAG}
      dir('BidClips-Infrastructure') {
        deleteDir()
      }
      dir('BidClips-Web-Provider-Portal') {
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
