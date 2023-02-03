node("built-in"){
  try{
    properties([
      authorizationMatrix([
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:NidhiPrajapati2410',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:birengreylab',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:kshraval',
        'USER:com.cloudbees.plugins.credentials.CredentialsProvider.View:rushitpadia',
        'USER:hudson.model.Item.Build:kshraval',
        'USER:hudson.model.Item.Cancel:kshraval',
        'USER:hudson.model.Item.Read:NidhiPrajapati2410',
        'USER:hudson.model.Item.Read:birengreylab',
        'USER:hudson.model.Item.Read:kshraval',
        'USER:hudson.model.Item.Read:rushitpadia',
        'USER:hudson.model.Item.Workspace:kshraval'
      ]),
      buildDiscarder(logRotator(numToKeepStr: '10')),
      disableConcurrentBuilds(abortPrevious: false),
      disableResume(),
      parameters([
        choice(choices: ['dev', 'qa', 'uat', 'prod'], description: '', name: 'DeployEnv'),
        [$class: 'ListSubversionTagsParameterDefinition', credentialsId: 'munjal-gc-un-pw', name: 'TagName', reverseByDate: true, reverseByName: false, tagsDir: 'https://github.com/BidClips/BidClips-Web-Gateway.git']
      ])
    ])
    def DEPLOYTAG = ""
    def repoRegion = ""
    def bootstrapper = [
      "dev": "18.141.143.199",
      "qa": "18.141.143.199",
      "uat": "18.141.143.199",
      "prod": "18.141.143.199"
    ]
    def DOMAIN = ""
    def dockerImageWithTag = ""
    def manifest_path = "BidClips-EKS/Kubernetes/application-stack/"
    def java_options = "-Xmx870m"
    def thread_pool_core_size = '5'
    def thread_pool_max_size = '5'
    def stale_data_change_event = ""
    def active_data_change_event = "";

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
        dir('BidClips-Web-Gateway') {
          if (TagName.startsWith('branches')) {
            branch = TagName.split('/')[1]
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Gateway.git']]]
          }
          if (TagName.equals('trunk')) {
            TagName = 'branches/master'
            checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Web-Gateway.git']]]
          }
          DEPLOYTAG = sh(
            script: 'echo $(git log -1 --pretty=%h)',
            returnStdout: true
          ).trim()
          repoRegion = "ap-southeast-1"
          deleteDir()
        }
      }
      dir('BidClips-Infrastructure'){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]

        // if (TagName.startsWith('tags')) {
        //   checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'refs/${TagName}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        // }
        // if (TagName.startsWith('branches')) {
        //   def branch = TagName.split('/')[1]
        //   checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: branch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        // }
        // if (TagName.equals('trunk')) {
        //   TagName = 'branches/master'
        //   checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        // }

        if(DeployEnv == "prod"){
          dockerImageWithTag="875588116685.dkr.ecr.us-east-1.amazonaws.com/bidclips-web-gateway:${DEPLOYTAG}".replace(':','\\:')
        }
        else{
          dockerImageWithTag="566570633830.dkr.ecr.${repoRegion}.amazonaws.com/bidclips-web-gateway:${DEPLOYTAG}".replace(':','\\:')
        }

        def mongo_uri_id = ''
        def eureka_defaultzone = ''
        def database = ''
        def mail_host = ''
        def mail_username = ''
        def mail_password = ''
        def ssl_trust = ''
        def sendgrid_password = ''
        def web_pushkey_public =''
        def web_pushkey_private = ''
        def zipkin_base_url = ''
        def secret = ''
        def mail_from = ''
        def mail_base_URL = ''
        def twilio_account_sid = ''
        def twilio_api_key = ''
        def twilio_api_secret = ''
        def twilio_chat_service_sid = ''
        def twilio_auth_token = ''
        def twilio_phone_number = ''
        def awss3_accesskey = ''
        def awss3_secretkey = ''
        def awslambda_accesskey = ''
        def awslambda_secretkey = ''
        def aws3javascript_accesskey = ''
        def aws3javascript_secretkey = ''
        def aws3javascript_bucket = ''
        def aws3javascript_URL = ''
        def aws3javascript_region = ''
        def functionNameImageRotation = ''
        def awscloud_accesskey = ''
        def awscloud_secretkey = ''
        def bucketname = ''
        def data_change_event = ''
        def v2_data_change_event = ''
        def aws_s3_region_id = ''
        def aws_lambda_region_id = ''
        def aws_cloud_region_id = ''
        def restheart_url = ''
        def mainstreet_url = ''
        def awscf_distribution_id = ''
        def awscf_accesskey = ''
        def awscf_secretkey = ''
        def awscf_region = ''
        def custom_mail_from = ''
        def custom_mail_base_url = ''
        def custom_mail_domain = ''
        def custom_mail_user = ''
        def custom_mail_shop_domain = ''
        def twilio_sms_account_sid = ''
        def twilio_sms_auth_token = ''
        def twilio_sms_webhook_url = ''
        def quickbook_url = ''

        if (DeployEnv == 'qa') {
          mongo_uri_id = 'c30e77e1-8741-452f-ba58-f4d0724eb1c4'
          eureka_defaultzone = '39f404a8-48eb-46a3-9546-ebc9085c991a'
          database = 'ff82a393-4078-4c50-8e5d-c7efb0974cb2'
          mail_host = '90ab4109-54b1-4382-b6ec-caee84620c98'
          mail_username = 'c8616485-1fb9-4b29-a463-13e0a3921342'
          mail_password = '2aaa74a7-77b3-4885-abdb-48293f5bed90'
          ssl_trust = 'caa2d16d-2385-4d6e-8ef5-4912e0939f1c'
          sendgrid_password = 'c0a42e7d-1510-4bd8-9b74-6f7e1ea2d832'
          web_pushkey_public = 'ea5ff041-257e-4ce6-bbc5-b615de2cd314'
          web_pushkey_private = 'fb590e8c-a20d-461e-acdb-017605a3294f'
          zipkin_base_url = 'd87c9048-5a35-491f-8c8a-1e6cbae3ab66'
          secret = '49fe8b37-ac9c-4723-8e88-8833fffc52fe'
          mail_from = '3ba32f0c-32f8-415f-9902-e2b2bfffb23e'
          mail_base_URL = 'beb57e40-79f2-4b2e-a092-5e3b45c6a59b'
          twilio_account_sid = '750e7c94-c8d2-4e13-9337-c07a897101c0'
          twilio_api_key = 'deeec09c-f63f-4805-8ea5-8de19fbf61b2'
          twilio_api_secret = '2941b8e8-0101-4ec2-8eb5-bfed05c4dcd4'
          twilio_chat_service_sid = '4dfdcaa0-c024-4bdf-847a-f9b5426f8848'
          twilio_auth_token = 'f8c150a6-38ed-48ec-956a-088c4bde7929'
          twilio_phone_number = 'e2b373b0-b72a-4616-9b6a-e01e83615529'
          awss3_accesskey = 'c34b4c04-f7f0-4988-9d92-96369b752607'
          awss3_secretkey = 'b461af82-7571-4612-bcb7-ac7856ea7271'
          awslambda_accesskey = 'f7a26624-414e-41f4-acb6-085e41dd2aba'
          awslambda_secretkey = '23bb04f7-fe8b-45c5-ae51-f74295e78c5a'
          aws3javascript_accesskey = 'c093d1d5-b05f-406c-8cb6-ca98e16e15d9'
          aws3javascript_secretkey = 'f34ff0ff-32fe-41f3-806a-5014c7feaa27'
          aws3javascript_bucket = '5e5cc6f9-ca3d-424b-99ce-82a64e490d2e'
          aws3javascript_URL = '3540abf6-55f8-42e8-8cbd-6167bec80643'
          aws3javascript_region = '6c654ff3-fd05-41a3-b16a-749cacd3003b'
          functionNameImageRotation = '224078f7-ebcb-4441-8633-be11a4d058c5'
          awscloud_accesskey = 'd0193cf6-b9c3-4f50-b6fa-5145ecba6f29'
          awscloud_secretkey = 'd0e318dc-a970-4a96-82d9-72fa07e03057'
          bucketname = 'f79e8cfa-aac0-4f97-96b4-5384bac9a454'
          data_change_event = '992d4d04-61f8-4b71-bd8b-fae92215b276'
          v2_data_change_event = 'f279d3ae-8423-4d86-9bb2-623177d743ae'
          aws_s3_region_id = '07b45476-6bc5-4883-a8d6-9aaf2c05baa6'
          aws_lambda_region_id = '07b45476-6bc5-4883-a8d6-9aaf2c05baa6'
          aws_cloud_region_id = '07b45476-6bc5-4883-a8d6-9aaf2c05baa6'
          restheart_url = 'd7c6eeed-22ae-49ca-ad39-a1f4a36ca096'
          mainstreet_url = '3ea3bdf7-5d31-4be8-bccd-6ebe3abaf661'
          awscf_distribution_id = 'fe4b0d6a-f735-4fcc-b0cd-f3c90d26829e'
          awscf_accesskey = 'acf6563b-6475-4449-a9c2-bccc3cbef6b0'
          awscf_secretkey = '84a56480-c2dd-46d1-9dac-d561116df71d'
          awscf_region = 'df0921c9-4046-4708-9796-92a0d4be9d95'
          custom_mail_from = "do-not-reply@bidclips.tech"
          custom_mail_base_url = "https://providers.bidclips.tech"
          custom_mail_domain = "bidclips.tech"
          custom_mail_user = "do-not-reply"
          custom_mail_shop_domain = "shop.bidclips.tech"
          twilio_sms_account_sid = '261392e2-f61c-468a-a170-6022cd1e3536'
          twilio_sms_auth_token = 'b6fd3ff8-c249-415b-a4fb-c4b99fff36ff'
          twilio_sms_webhook_url = '36b718b7-f4e1-46d2-a2ae-f9cf73d48ca6'
          quickbook_url = "https://providers.bidclips.tech/api/quickbook"
        }

        if (DeployEnv == 'uat') {
          mongo_uri_id = '2311be07-35c4-4296-ba5b-a11e637e1787'
          eureka_defaultzone = '2a758f4b-ac28-43e2-bf92-1e0fce8ca508'
          database = 'ff82a393-4078-4c50-8e5d-c7efb0974cb2'
          mail_host = '9489f497-8e27-4241-b1a9-d86ac1e09718'
          mail_username = '8e5ca14c-0bda-443d-8b3c-b5c9454528e8'
          mail_password = '8425099b-66c5-4992-b9a3-0d3d4f1509a5'
          ssl_trust = 'caa2d16d-2385-4d6e-8ef5-4912e0939f1c'
          sendgrid_password = '9fa3fcad-1fc2-4837-ab50-e415ea7036fc'
          web_pushkey_public = 'fb313725-3c09-41f3-9acf-c4f01b3e3a0f'
          web_pushkey_private = '604b2889-05ec-4e47-b3f8-c02b2a33c174'
          zipkin_base_url = 'd87c9048-5a35-491f-8c8a-1e6cbae3ab66'
          secret = '6852d2ad-9d2e-45ef-8df4-74a9e75db153'
          mail_from = 'b20cc353-2c59-42f1-b25e-bc36861a8e40'
          mail_base_URL = 'c8df649a-a6ce-4dc1-8e0b-6a6ae6a859b3'
          twilio_account_sid = '67f561ba-af72-4cf0-a5eb-88981e54900c'
          twilio_api_key = '0d8397a6-e20d-4447-b03c-849ed18547c4'
          twilio_api_secret = 'e261513f-16a9-44ec-b30a-27f413c8c0e6'
          twilio_chat_service_sid = '4857e5f2-3d2c-4d54-a1f5-515e91a0810a'
          twilio_auth_token = 'ce1b15b7-f4ac-4dfc-ab75-e1b12ceaf9f6'
          twilio_phone_number = 'e10c2b84-20b2-4501-b980-2a10d2bd0d8c'
          awss3_accesskey = '3027b327-b28e-41e5-94b9-715b11034607'
          awss3_secretkey = '3d7a5dce-8f0f-4dcc-94ff-c2c6aeb6039a'
          awslambda_accesskey = '7be0fac0-1b64-4670-9807-6b5e18d92b2c'
          awslambda_secretkey = '1900797d-162c-47c0-a0c7-ba1c7dcfbc43'
          aws3javascript_accesskey = 'c093d1d5-b05f-406c-8cb6-ca98e16e15d9'
          aws3javascript_secretkey = 'f34ff0ff-32fe-41f3-806a-5014c7feaa27'
          aws3javascript_bucket = '2dff6980-818d-4bfc-a9cb-6124783d798b'
          aws3javascript_URL = '330171d5-ddd0-4a7f-8c0b-bb1a10a6c3e6'
          aws3javascript_region = '595c05d6-6979-4d1e-824b-967a14707950'
          functionNameImageRotation = '8d9f2531-51b0-4d34-92af-3a948b45069e'
          awscloud_accesskey = 'f0963845-50ed-4edc-9500-09601269a754'
          awscloud_secretkey = '5ee95075-a623-4517-8832-20df25f17ca6'
          bucketname = '8244aaaf-e722-4928-b1ca-79bbf53d0a68'
          data_change_event = 'c771286a-b8dd-4cba-bedc-49273f7fc13a'
          v2_data_change_event = '2471e5fd-00fb-49b1-b802-9624ccc72014'
          aws_s3_region_id = 'a7e9b79d-b3e0-411d-82a0-56d3ffa551ca'
          aws_lambda_region_id = 'a7e9b79d-b3e0-411d-82a0-56d3ffa551ca'
          aws_cloud_region_id = 'a7e9b79d-b3e0-411d-82a0-56d3ffa551ca'
          restheart_url = '3c287199-5a6f-4759-b4cb-43cbc0dd0a2c'
          mainstreet_url = '3921924a-267f-4e52-bca3-eb181d5ea341'
          awscf_distribution_id = '3fb06cdf-26fb-489e-b0c1-8c7b761e8caa'
          awscf_accesskey = 'e237838c-6d7b-430d-84bb-8a6b246ac63d'
          awscf_secretkey = 'c8573acb-c5ce-4b62-af33-190504c205ff'
          awscf_region = '068e0464-4bff-4517-8250-97280ed7d78c'
          custom_mail_from = "do-not-reply@bidclipsuat.com"
          custom_mail_base_url = "https://providers.bidclipsuat.com"
          custom_mail_domain = "bidclipsuat.com"
          custom_mail_user = "do-not-reply"
          custom_mail_shop_domain = "shop.bidclipsuat.com"
          twilio_sms_account_sid = 'f1c4ba8c-2efd-48f4-867e-0f16ae89ee7f'
          twilio_sms_auth_token = 'da41a78a-eb55-4b13-9638-568d47d36256'
          twilio_sms_webhook_url = 'ee7e538d-a1a7-413c-8f0a-fa49244349e4'
          quickbook_url = "https://providers.bidclipsuat.com/api/quickbook"
        }

        if (DeployEnv == 'dev') {
          mongo_uri_id = 'fa0513f2-3763-4c81-ac42-fb1945efbd2a'
          eureka_defaultzone = 'cc22e9c8-b47e-4d5f-9512-a857afc7cfe4'
          database = 'ff82a393-4078-4c50-8e5d-c7efb0974cb2'
          mail_host = '7fcd4c08-883e-4e11-9a96-a2fb6f738999'
          mail_username = '87332d00-1d31-45df-9a87-87df01e6da2d'
          mail_password = '9f169fc7-3dfc-433e-a54d-09522a9d2278'
          ssl_trust = 'caa2d16d-2385-4d6e-8ef5-4912e0939f1c'
          sendgrid_password = 'b964f09d-4a16-492d-bf7a-c39c45c59c6d'
          web_pushkey_public = '2b2e2dd3-ba5d-41f3-8dd7-03c2eba51547'
          web_pushkey_private = '70bd393d-5415-4d9b-bcc3-05798871ab05'
          zipkin_base_url = 'd87c9048-5a35-491f-8c8a-1e6cbae3ab66'
          secret = '43fc943b-3902-4fa7-ae03-0098d74fc60f'
          mail_from = '69fa9e7d-f13d-4029-a495-71609ea09e70'
          mail_base_URL = '42bb4412-cad8-4c59-a7d9-ef5893354547'
          twilio_account_sid = 'fb29b9e2-b1b8-4344-9cf8-9704907cea0d'
          twilio_api_key = 'ce0dc033-4d2d-4a11-82fc-2ab122177a32'
          twilio_api_secret = '21be1aa2-02af-40bf-a16f-944235251000'
          twilio_chat_service_sid = '58a31661-c6dd-44f4-96dd-f035b9e6fff5'
          twilio_auth_token = 'dc0babe8-5cfa-4072-a92c-a499f4f7d453'
          twilio_phone_number = '174947c2-32cc-4808-a7c0-2dcd465cfa77'
          awss3_accesskey = '098ee31a-fc72-4abb-a888-407a2548fc02'
          awss3_secretkey = '1c341e71-e3bf-48e3-8414-20cebdff1ab6'
          awslambda_accesskey = '00b9fae7-8c2c-4648-aaca-85011124c2e7'
          awslambda_secretkey = 'cf0de200-5cea-4199-bdff-5113be18a01e'
          aws3javascript_accesskey = 'c093d1d5-b05f-406c-8cb6-ca98e16e15d9'
          aws3javascript_secretkey = 'f34ff0ff-32fe-41f3-806a-5014c7feaa27'
          aws3javascript_bucket = '2206dfd8-1023-49a3-afc9-68df2296479d'
          aws3javascript_URL = 'dc8d42eb-aa2c-4ef2-927a-0c8204efb57d'
          aws3javascript_region = '6c654ff3-fd05-41a3-b16a-749cacd3003b'
          functionNameImageRotation = 'bc938dd3-de59-49c9-b52d-8064675b80fe'
          awscloud_accesskey = '6074badb-a772-4c6b-9fcb-499f19e97c27'
          awscloud_secretkey = '5a85fb10-c72e-4c8f-9b78-86bd8539d476'
          bucketname = '9f26dc63-b0c9-40fc-8343-df7a6410648e'
          data_change_event = '15460e24-d92d-4f60-b022-aab48f726012'
          v2_data_change_event = 'b3e77a11-b604-44e6-b89b-d657d442ec9d'
          aws_s3_region_id = '053fc65c-29e4-48fc-b3a1-fc34a4c5f8f8'
          aws_lambda_region_id = '609c3879-a4f4-4f80-8527-17637cfa90f0'
          aws_cloud_region_id = '1218705f-af68-4cf0-a5df-3aea589f4525'
          restheart_url = '7a3edf37-bb4a-4a95-9f08-02f662762e02'
          mainstreet_url = '8609b992-0146-45c5-87a2-705ee747d671'
          awscf_distribution_id = '59baa2b9-c7cd-4826-a248-52a5ca103187'
          awscf_accesskey = 'cad021ff-5eb3-4182-8dd8-13530e8fe489'
          awscf_secretkey = '27c057dc-3786-465a-80fe-52d21c34ecea'
          awscf_region = '5b55ba1d-04b4-4548-90d9-85969cadab04'
          custom_mail_from = "do-not-reply@bidclips.dev"
          custom_mail_base_url = "https://providers.bidclips.dev"
          custom_mail_domain = "bidclips.dev"
          custom_mail_user = "do-not-reply"
          custom_mail_shop_domain = "shop.bidclips.dev"
          twilio_sms_account_sid = 'a469c4ee-f928-4093-9441-0d8995a8a633'
          twilio_sms_auth_token = '904efafc-d8c9-472e-8371-8252dfab2caa'
          twilio_sms_webhook_url = 'c0e212d4-bc6b-4502-9603-045f0c164b23'
          quickbook_url = "https://providers.bidclips.dev/api/quickbook"
        }

        if (DeployEnv == 'prod') {
          mongo_uri_id = 'e1af3291-3e80-49c8-b5d0-77cd8cb6119c'
          eureka_defaultzone = '9336844f-9846-4fe9-95ba-45db6a50e6bf'
          database = 'ff82a393-4078-4c50-8e5d-c7efb0974cb2'
          mail_host = '94244753-8502-4040-b542-275809515cde'
          mail_username = '40cec108-237a-472e-a019-15bea50c4fd0'
          mail_password = 'd7f5cf74-e5fe-417d-a0d5-6171bf60258f'
          ssl_trust = 'e0a19af5-c73a-468f-80af-d8586d7f8411'
          sendgrid_password = 'b51b5c08-11b0-40e1-befd-56660adbe760'
          web_pushkey_public = 'f8282993-7711-4ca3-9eb4-aea4ef3f4fb4'
          web_pushkey_private = '2e8312ca-e999-48d9-afe8-87f27f36c9af'
          zipkin_base_url = 'd87c9048-5a35-491f-8c8a-1e6cbae3ab66'
          secret = '0e3deb42-4fc5-4d1b-b6bd-91019debae9e'
          mail_from = 'e36e41e1-df6b-4716-8256-ec69d3c555fc'
          mail_base_URL = 'abf0f793-2de2-4c9f-8e52-13af3c5b1ca0'
          twilio_account_sid = 'cf7bc0bc-7dd7-4a7f-92f8-e7e877805f25'
          twilio_api_key = '9c8ed0f4-4c24-4ecd-a1dd-3bbc10082376'
          twilio_api_secret = '34dfed11-77c6-4396-823a-99775e0d0425'
          twilio_chat_service_sid = 'eb6abffa-48ea-42b1-895c-557415b79707'
          twilio_auth_token = 'cc2aecc0-a0e2-4bf6-b561-311981d61d41'
          twilio_phone_number = '03ce470f-e42b-4809-bec8-672f97d692e3'
          awss3_accesskey = '697cfc74-f7ba-46f8-acf3-4afd35bf08f7'
          awss3_secretkey = 'dd7abf88-42bf-42d2-b8a1-e4d3f5d933f0'
          awslambda_accesskey = '293ce467-7da2-4ca4-8e9c-b28ea8c3b821'
          awslambda_secretkey = '1e74baf7-6efe-4d88-bd1d-fb7214c9ab48'
          aws3javascript_accesskey = '4baf58e6-6164-4f89-ac9a-cd4cde4dd148'
          aws3javascript_secretkey = '1f76e162-7e0c-497d-a419-eba030c5d4db'
          aws3javascript_bucket = '3c4a2f18-dba7-482c-89f0-1b0d742407c3'
          aws3javascript_URL = '6b003b64-752f-49da-ab08-961ac944abe7'
          aws3javascript_region = '595c05d6-6979-4d1e-824b-967a14707950'
          functionNameImageRotation = 'cd76bf7b-0ae5-4f00-aff8-66c943e7d27e'
          awscloud_accesskey = '5a4ec2b4-e19f-41fb-8136-6b5b124266e7'
          awscloud_secretkey = '9d90f3e2-61f7-42fd-af23-903f98ea9921'
          bucketname = '2109839b-c9b2-4cc1-9a2e-c5ed4067a1d0'
          data_change_event = '5a4b0a0c-569d-41f8-862a-98428875f896'
          v2_data_change_event = '7166d8c2-c01c-4cc5-b775-77cd606a4e7b'
          aws_s3_region_id = '0bcb35cb-e010-4635-9819-2135fcbe4fbf'
          aws_lambda_region_id = '0bcb35cb-e010-4635-9819-2135fcbe4fbf'
          aws_cloud_region_id = '0bcb35cb-e010-4635-9819-2135fcbe4fbf'
          restheart_url = 'c0d52f8d-bf6e-418b-93e2-6d1048130843'
          mainstreet_url = '97ed71ac-1073-447f-a165-025d150d0750'
          // cloudfront distribution info needs to be changed for Prod
          awscf_distribution_id = '2123f876-8d67-4ea3-b073-4be40e3bd14c'
          awscf_accesskey = 'cce1f313-8774-48dc-9d59-f009f6ff6d03'
          awscf_secretkey = 'df50efca-1e39-419e-85a2-1b2910f283b8'
          awscf_region = '29fc9b1b-f183-44ce-912b-bb370e579500'
          custom_mail_from = "do-not-reply@bidclips.com"
          custom_mail_base_url = "https://providers.bidclips.com"
          custom_mail_domain = "bidclips.com"
          custom_mail_user = "do-not-reply"
          custom_mail_shop_domain = "shop.bidclips.com"
          twilio_sms_account_sid = '63243413-eb7e-40a2-9d8f-3e2ffff3bcd9'
          twilio_sms_auth_token = '92113db6-4988-4f9c-a49f-46f0cf66cc13'
          twilio_sms_webhook_url = 'a11fe8ba-ef74-4756-bee4-8c366bcf79e1'
          java_options = "-Xmx2560m -Xlog:gc*:file=/var/log/bidclips-web-gateway-gc.log"
          thread_pool_core_size = '8'
          thread_pool_max_size = '15'
          manifest_path = "BidClips-EKS/Kubernetes/application-stack-prod/"
          quickbook_url = "https://providers.bidclips.com/api/quickbook"
        }

        withCredentials([
          string(credentialsId: mongo_uri_id, variable: 'var_mongo_uri_id'),
          string(credentialsId: eureka_defaultzone, variable: 'var_eureka_defaultzone'),
          string(credentialsId: database, variable: 'var_database'),
          string(credentialsId: mail_host, variable: 'var_mail_host'),
          string(credentialsId: mail_username, variable: 'var_mail_username'),
          string(credentialsId: mail_password, variable: 'var_mail_password'),
          string(credentialsId: ssl_trust, variable: 'var_ssl_trust'),
          string(credentialsId: sendgrid_password, variable: 'var_sendgrid_password'),
          string(credentialsId: web_pushkey_private, variable: 'var_web_pushkey_private'),
          string(credentialsId: web_pushkey_public, variable: 'var_web_pushkey_public'),
          string(credentialsId: zipkin_base_url, variable: 'var_zipkin_base_url'),
          string(credentialsId: secret, variable: 'var_secret'),
          string(credentialsId: mail_from, variable: 'var_mail_from'),
          string(credentialsId: mail_base_URL, variable: 'var_mail_base_URL'),
          string(credentialsId: twilio_account_sid, variable: 'var_twilio_account_sid'),
          string(credentialsId: twilio_api_key, variable: 'var_twilio_api_key'),
          string(credentialsId: twilio_api_secret, variable: 'var_twilio_api_secret'),
          string(credentialsId: twilio_chat_service_sid, variable: 'var_twilio_chat_service_sid'),
          string(credentialsId: twilio_auth_token, variable: 'var_twilio_auth_token'),
          string(credentialsId: twilio_phone_number, variable: 'var_twilio_phone_number'),
          string(credentialsId: awss3_accesskey, variable: 'var_awss3_accesskey'),
          string(credentialsId: awss3_secretkey, variable: 'var_awss3_secretkey'),
          string(credentialsId: awslambda_accesskey, variable: 'var_awslambda_accesskey'),
          string(credentialsId: awslambda_secretkey, variable: 'var_awslambda_secretkey'),
          string(credentialsId: aws3javascript_accesskey, variable: 'var_aws3javascript_accesskey'),
          string(credentialsId: aws3javascript_secretkey, variable: 'var_aws3javascript_secretkey'),
          string(credentialsId: aws3javascript_bucket, variable: 'var_aws3javascript_bucket'),
          string(credentialsId: aws3javascript_URL, variable: 'var_aws3javascript_URL'),
          string(credentialsId: aws3javascript_region, variable: 'var_aws3javascript_region'),
          string(credentialsId: functionNameImageRotation, variable: 'var_functionNameImageRotation'),
          string(credentialsId: awscloud_accesskey, variable: 'var_awscloud_accesskey'),
          string(credentialsId: awscloud_secretkey, variable: 'var_awscloud_secretkey'),
          string(credentialsId: bucketname, variable: 'var_bucketname'),
          string(credentialsId: data_change_event, variable: 'var_data_change_event'),
          string(credentialsId: v2_data_change_event, variable: 'var_v2_data_change_event'),
          string(credentialsId: aws_s3_region_id, variable: 'var_aws_s3_region_id'),
          string(credentialsId: aws_lambda_region_id, variable: 'var_aws_lambda_region_id'),
          string(credentialsId: aws_cloud_region_id, variable: 'var_aws_cloud_region_id'),
          string(credentialsId: restheart_url, variable: 'var_restheart_url'),
          string(credentialsId: mainstreet_url, variable: 'var_mainstreet_url'),
          string(credentialsId: awscf_distribution_id, variable: 'var_awscf_distribution_id'),
          string(credentialsId: awscf_accesskey, variable: 'var_awscf_accesskey'),
          string(credentialsId: awscf_secretkey, variable: 'var_awscf_secretkey'),
          string(credentialsId: awscf_region, variable: 'var_awscf_region'),
          string(credentialsId: twilio_sms_account_sid, variable: 'var_twilio_sms_account_sid'),
          string(credentialsId: twilio_sms_auth_token, variable: 'var_twilio_sms_auth_token'),
          string(credentialsId: twilio_sms_webhook_url, variable: 'var_twilio_sms_webhook_url')
        ]) {
            var_eureka_defaultzone=var_eureka_defaultzone.replace('/','\\/')
            var_eureka_defaultzone=var_eureka_defaultzone.replace('{','\\{')
            var_eureka_defaultzone=var_eureka_defaultzone.replace('}','\\}')
            var_eureka_defaultzone=var_eureka_defaultzone.replace('$','\\$')
            var_eureka_defaultzone=var_eureka_defaultzone.replace('@','\\@')
            var_eureka_defaultzone=var_eureka_defaultzone.replace(':','\\:')
            var_mongo_uri_id=var_mongo_uri_id.replace('&','\\&')
            custom_mail_from=custom_mail_from.replace('@','\\@')
            custom_mail_base_url=custom_mail_base_url.replace('/','\\/')
            var_twilio_sms_webhook_url=var_twilio_sms_webhook_url.replace('/','\\/')
            var_twilio_sms_webhook_url=var_twilio_sms_webhook_url.replace(':','\\:')
            stale_data_change_event = var_data_change_event
            active_data_change_event = var_v2_data_change_event
          sh """
          ls -alh common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MONGODB_CONNECTION_URL#$var_mongo_uri_id#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_EUREKA_DEFAULTZONE#$var_eureka_defaultzone#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_DATABASE#$var_database#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAIL_HOST#$var_mail_host#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAIL_USERNAME#$var_mail_username#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAIL_PASSWORD#$var_mail_password#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_SSL_TRUST#$var_ssl_trust#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_SENDGRID_PASSWORD#$var_sendgrid_password#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_WEBPUSH_PRIVATE_KEY#$var_web_pushkey_private#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_WEBPUSH_PUBLIC_KEY#$var_web_pushkey_public#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_ZIPKIN_BASE_URL#$var_zipkin_base_url#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_SECRET#$var_secret#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAIL_FROM#$var_mail_from#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAIL_BASE_URL#$var_mail_base_url#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_ACCOUNT_SID#$var_twilio_account_sid#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_API_KEY#$var_twilio_api_key#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_API_SECRET#$var_twilio_api_secret#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_CHAT_SERVICE_SID#$var_twilio_chat_service_sid#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_AUTH_TOKEN#$var_twilio_auth_token#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_PHONE_NUMBER#$var_twilio_phone_number#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_AWSS3_ACCESSKEY#$var_awss3_accesskey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_AWSS3_SECRETKEY#$var_awss3_secretkey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSLAMBDA_ACCESSKEY#$var_awslambda_accesskey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSLAMBDA_SECRETKEY#$var_awslambda_secretkey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS3JAVASCRIPT_ACCESSKEY#$var_aws3javascript_accesskey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS3JAVASCRIPT_SECRETKEY#$var_aws3javascript_secretkey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS3JAVASCRIPT_REGION#$var_aws3javascript_region#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS3JAVASCRIPT_BUCKET#$var_aws3javascript_bucket#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS3JAVASCRIPT_URL#$var_aws3javascript_URL#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_FUNCTIONNAMEIMAGEROTATION#$var_functionNameImageRotation#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSCLOUD_ACCESSKEY#$var_awscloud_accesskey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSCLOUD_SECRETKEY#$var_awscloud_secretkey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_BUCKETNAME#$var_bucketname#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_DATA_CHANGE_EVENT#$var_data_change_event#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_V2_DATA_CHANGE_EVENT#$var_v2_data_change_event#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSS3_REGION_ID#$var_aws_s3_region_id#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSLAMBDA_REGION_ID#$var_aws_lambda_region_id#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWSCLOUD_REGION_ID#$var_aws_cloud_region_id#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_RESTHEART_URL#$var_restheart_url#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_MAINSTREET_URL#$var_mainstreet_url#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_CLOUDFRONT_DISTRIBUTION_ID#$var_awscf_distribution_id#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_CLOUDFRONT_ACCESSKEY#$var_awscf_accesskey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_CLOUDFRONT_SECRETKEY#$var_awscf_secretkey#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_AWS_CLOUDFRONT_REGION#$var_awscf_region#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_CUSTOM_MAIL_FROM#${custom_mail_from}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_CUSTOM_MAIL_BASE_URL#${custom_mail_base_url}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_CUSTOM_MAIL_DOMAIN#${custom_mail_domain}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_CUSTOM_MAIL_USER#${custom_mail_user}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_CUSTOM_MAIL_SHOP_DOMAIN#${custom_mail_shop_domain}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_SMS_ACCOUNT_SID#$var_twilio_sms_account_sid#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_SMS_AUTH_TOKEN#$var_twilio_sms_auth_token#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_TWILIO_SMS_WEBHOOK_URL#$var_twilio_sms_webhook_url#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_THREAD_POOL_CORE_SIZE#${thread_pool_core_size}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_THREAD_POOL_MAX_SIZE#${thread_pool_max_size}#g' common/BidClips-Web-Gateway-provider/application-dev.yml
          sed -i 's#REPLACEME_QUICKBOOK_URL#${quickbook_url}#g' common/BidClips-Web-Gateway-provider/application-dev.yml


          mkdir gateway-config/
          cp common/BidClips-Web-Gateway-provider/application-dev.yml gateway-config/application-dev.yml
          cp gateway-config/application-dev.yml gateway-config/application-prod.yml
          cp common/BidClips-Web-Gateway-provider/logback.xml gateway-config/logback-spring.xml
          tar -czf gateway-config.tar.gz gateway-config/
          rm -rf gateway-config/
          scp gateway-config.tar.gz appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/gateway-config.tar.gz
          rm gateway-config.tar.gz
          """
        }
      }
      dir("BidClips-EKS-Manifest"){
        checkout poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'munjal-gc', url: 'git@github.com:BidClips/BidClips-Infrastructure.git']]]
        java_options=java_options.replace('/','\\/')
        java_options=java_options.replace(':','\\:')
        sh """
echo ${java_options}
echo ${manifest_path}
cd ${manifest_path}
ls -lh gateway*
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#${dockerImageWithTag}#g' gateway.yaml
sed -i 's#REPLACEME_JAVA_OPTIONS#${java_options}#g' gateway.yaml
sed -i 's#REPLACEME_DATA_CHANGE_EVENT#${stale_data_change_event}#g' gateway.yaml
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#${dockerImageWithTag}#g' gateway-internal.yaml
sed -i 's#REPLACEME_DATA_CHANGE_EVENT#${stale_data_change_event}#g' gateway-internal.yaml
scp gateway.yaml appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/gateway.yaml
scp gateway-internal.yaml appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/gateway-internal.yaml
if [ -f 'gateway-backend.yaml' ];then
sed -i 's#REPLACEME_DOCKER_IMAGE_WITH_TAG#${dockerImageWithTag}#g' gateway-backend.yaml
sed -i 's#REPLACEME_JAVA_OPTIONS#${java_options}#g' gateway-backend.yaml
sed -i 's#REPLACEME_DATA_CHANGE_EVENT#${active_data_change_event}#g' gateway-backend.yaml
scp gateway-backend.yaml appuser@${bootstrapper.get(DeployEnv)}:/home/appuser/gateway-backend.yaml
fi
        """
      }
    }

    stage("Deploying ${DEPLOYTAG}"){
      sh """
ssh -tt appuser@${bootstrapper.get(DeployEnv)} /bin/bash << EOA
export AWS_DEFAULT_REGION="${repoRegion}"
ls -lh gateway*
tar -xzf gateway-config.tar.gz
rm gateway-config.tar.gz
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack delete configmap gateway-config
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack create configmap gateway-config --from-file=gateway-config/
sleep 5;
rm -rf gateway-config/
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config apply -f gateway.yaml -f gateway-internal.yaml
if [ -f 'gateway-backend.yaml' ];then
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config apply -f gateway-backend.yaml
fi
rm gateway*
sleep 5;
kubectl --kubeconfig=/home/appuser/.kube/bidclips_${DeployEnv}_config -n app-stack get deploy | grep gateway
exit
EOA
      """
    }
    stage("cleanup"){
      dir("BidClips-EKS-Manifest"){
        deleteDir()
      }
      dir("BidClips-Web-Gateway"){
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
