// =============================================================================
// Jenkinsfile — fash-android-mobile (Gradle / APK + AAB / Google Play)
//
// Runs alongside GitHub Actions — does NOT replace .github/workflows/*.yml.
// GitLab → GitHub mirror can keep using Actions; Jenkins builds from GitLab directly.
//
// Prerequisites (Jenkins agent — Linux recommended):
//   - JDK 17 (Temurin), Android SDK (API 35+), ANDROID_HOME on PATH
//   - Docker (optional — Play upload via fastlanetools/fastlane if fastlane gem missing)
//   - Credentials:
//       • Git: CREDENTIALS_ID
//       • Secret file: env-fash-android-mobile-dev | env-fash-android-mobile-prod
//         (see secrets/jenkins-android.env.example)
//
// Job types (BUILD_MODE parameter):
//   dev-apk     — assembleDevRelease (like Android Build on develop)
//   prod-aab    — bundleProdRelease + optional Play upload
// =============================================================================

pipeline {

    agent any

    parameters {
        choice(
            name: 'BUILD_MODE',
            choices: ['dev-apk', 'prod-aab'],
            description: 'dev-apk = APK dev flavor; prod-aab = signed AAB (+ Play if enabled)'
        )
        choice(
            name: 'PLAY_TRACK',
            choices: ['FASH-production', 'alpha', 'internal', 'beta', 'production'],
            description: 'Google Play track (prod-aab + UPLOAD_PLAY only)'
        )
        booleanParam(
            name: 'UPLOAD_PLAY',
            defaultValue: true,
            description: 'Upload AAB to Google Play after bundle (prod-aab only)'
        )
        string(
            name: 'GIT_BRANCH',
            defaultValue: 'develop',
            trim: true,
            description: 'GitLab branch to build'
        )
    }

    environment {
        GIT_REPO       = 'https://gitlab.com/fash3194512/fash-android-mobile.git'
        CREDENTIALS_ID = 'e8689c9a-5588-4725-b8cd-712ae345d8e1'

        SLACK_CHANNEL   = '#ci-cd-alert'
        SLACK_ERROR_LOG = '#ci-cd-errors'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15'))
        timeout(time: 45, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
        disableConcurrentBuilds()
    }

    stages {

        stage('Notify Start') {
            steps {
                slackSend(
                    channel: env.SLACK_CHANNEL,
                    color: 'warning',
                    message: "🟡 *STARTED (fash-android):* `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n" +
                             "Mode: *${params.BUILD_MODE}* • Branch: *${params.GIT_BRANCH}*\n" +
                             "${env.BUILD_URL}"
                )
            }
        }

        stage('Checkout') {
            steps {
                cleanWs()
                git(
                    branch: params.GIT_BRANCH,
                    url: env.GIT_REPO,
                    credentialsId: env.CREDENTIALS_ID
                )
                script {
                    env.SHORT_COMMIT = sh(
                        script: 'git rev-parse --short=8 HEAD',
                        returnStdout: true
                    ).trim()
                    echo "✅ Checked out ${params.GIT_BRANCH} @ ${env.SHORT_COMMIT}"
                }
            }
        }

        stage('Prepare & Build') {
            steps {
                script {
                    def credId = params.BUILD_MODE == 'dev-apk' \
                        ? 'env-fash-android-mobile-dev' \
                        : 'env-fash-android-mobile-prod'

                    withCredentials([file(
                        credentialsId: credId,
                        variable: 'ENV_FILE'
                    )]) {
                        sh """#!/usr/bin/env bash
                            set -eo pipefail

                            STAMP="\${BUILD_NUMBER}-\$\$"
                            ENV_LF="\${WORKSPACE}/.jenkins-android.\${STAMP}.env"
                            cleanup() { rm -f "\${ENV_LF}" || true; }
                            trap cleanup EXIT

                            LC_ALL=C sed '1s/^\\xEF\\xBB\\xBF//' "\${ENV_FILE}" | tr -d '\\r' | \\
                                grep -v '^[[:space:]]*#' | grep -v '^[[:space:]]*\$' > "\${ENV_LF}"
                            set -a; source "\${ENV_LF}"; set +a

                            mkdir -p env
                            if [[ -n "\${ANDROID_DEV_ENV_B64:-}" ]]; then
                                echo "\${ANDROID_DEV_ENV_B64}" | base64 -d > env/dev.env
                            elif [[ -n "\${ANDROID_DEV_ENV:-}" ]]; then
                                printf '%s\\n' "\${ANDROID_DEV_ENV}" > env/dev.env
                            else
                                echo "[ERROR] Set ANDROID_DEV_ENV_B64 or ANDROID_DEV_ENV in ${credId}" >&2
                                exit 1
                            fi

                            if [[ -n "\${ANDROID_PROD_ENV_B64:-}" ]]; then
                                echo "\${ANDROID_PROD_ENV_B64}" | base64 -d > env/prod.env
                            elif [[ -n "\${ANDROID_PROD_ENV:-}" ]]; then
                                printf '%s\\n' "\${ANDROID_PROD_ENV}" > env/prod.env
                            else
                                echo "[ERROR] Set ANDROID_PROD_ENV_B64 or ANDROID_PROD_ENV in ${credId}" >&2
                                exit 1
                            fi

                            if command -v java >/dev/null 2>&1; then
                                java -version
                            else
                                echo "[ERROR] JDK 17 required on Jenkins agent" >&2
                                exit 1
                            fi

                            chmod +x gradlew scripts/ci_prepare_release_signing.sh scripts/ci_upload_google_play.sh

                            if [[ "${params.BUILD_MODE}" == "dev-apk" ]]; then
                                if [[ -n "\${ANDROID_UPLOAD_KEYSTORE_BASE64:-}" ]]; then
                                    export KEYSTORE_B64="\${ANDROID_UPLOAD_KEYSTORE_BASE64}"
                                    export STORE_PW="\${ANDROID_UPLOAD_KEYSTORE_PASSWORD:-}"
                                    export KEY_ALIAS="\${ANDROID_UPLOAD_KEY_ALIAS:-}"
                                    export KEY_PW="\${ANDROID_UPLOAD_KEY_PASSWORD:-}"
                                    bash scripts/ci_prepare_release_signing.sh || true
                                fi
                                ./gradlew :app:assembleDevRelease --stacktrace
                                ls -la app/build/outputs/apk/dev/release/ || true
                            else
                                export KEYSTORE_B64="\${ANDROID_UPLOAD_KEYSTORE_BASE64:-}"
                                export STORE_PW="\${ANDROID_UPLOAD_KEYSTORE_PASSWORD:-}"
                                export KEY_ALIAS="\${ANDROID_UPLOAD_KEY_ALIAS:-}"
                                export KEY_PW="\${ANDROID_UPLOAD_KEY_PASSWORD:-}"
                                bash scripts/ci_prepare_release_signing.sh
                                ./gradlew :app:bundleProdRelease --stacktrace
                                AAB="\$(find app/build/outputs/bundle/prodRelease -name '*.aab' | head -1)"
                                test -n "\${AAB}" && test -f "\${AAB}"
                                echo "\${AAB}" > "\${WORKSPACE}/.aab-path"
                                ls -la "\${AAB}"

                                if [[ "${params.UPLOAD_PLAY}" == "true" ]]; then
                                    export PLAY_TRACK="${params.PLAY_TRACK}"
                                    MAPPING="app/build/outputs/mapping/prodRelease/mapping.txt"
                                    if [[ -f "\${MAPPING}" ]]; then
                                        export PLAY_MAPPING_FILE="\${MAPPING}"
                                    fi
                                    bash scripts/ci_upload_google_play.sh "\${AAB}"
                                else
                                    echo "[INFO] Play upload skipped (UPLOAD_PLAY=false)"
                                fi
                            fi
                        """
                    }
                }
            }
            post {
                success {
                    archiveArtifacts(
                        artifacts: params.BUILD_MODE == 'dev-apk'
                            ? 'app/build/outputs/apk/dev/release/*.apk'
                            : 'app/build/outputs/bundle/prodRelease/*.aab,app/build/outputs/mapping/prodRelease/mapping.txt',
                        fingerprint: true,
                        allowEmptyArchive: false
                    )
                }
                failure {
                    slackSend(
                        channel: env.SLACK_ERROR_LOG,
                        color: 'danger',
                        message: "❌ *fash-android BUILD FAILED:* `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}console"
                    )
                }
            }
        }
    }

    post {
        success {
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: 'good',
                message: "✅ *fash-android SUCCESS:* `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n" +
                         "Mode: *${params.BUILD_MODE}* • Branch: *${params.GIT_BRANCH}* @ ${env.SHORT_COMMIT}\n" +
                         "${env.BUILD_URL}"
            )
        }
        failure {
            slackSend(
                channel: env.SLACK_CHANNEL,
                color: 'danger',
                message: "❌ *fash-android FAILED:* `${env.JOB_NAME}` #${env.BUILD_NUMBER}\n${env.BUILD_URL}console"
            )
        }
    }
}
