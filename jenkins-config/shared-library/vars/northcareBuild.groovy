/**
 * northcareBuild — builds a Docker image with BuildKit and labels it
 * with standard OCI image annotations for traceability.
 *
 * @param config.image        Full image name+tag (e.g. '123.dkr.ecr.../northcare/svc:42-abc')
 * @param config.serviceName  Service name used in build-arg and labels
 * @param config.dockerfile   Path to Dockerfile (default: 'Dockerfile')
 * @param config.context      Docker build context (default: '.')
 */
def call(Map config = [:]) {
    def image       = config.image       ?: error("northcareBuild: 'image' required")
    def serviceName = config.serviceName ?: env.SERVICE_NAME ?: 'unknown'
    def dockerfile  = config.dockerfile  ?: 'Dockerfile'
    def context     = config.context     ?: '.'

    def gitCommit = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
    def gitRepo   = sh(script: 'git remote get-url origin', returnStdout: true).trim()
    def buildDate = sh(script: 'date -u +"%Y-%m-%dT%H:%M:%SZ"', returnStdout: true).trim()

    sh """
        set -euo pipefail

        echo "Building Docker image: ${image}"
        docker build \\
            --file ${dockerfile} \\
            --tag "${image}" \\
            --build-arg SERVICE_NAME="${serviceName}" \\
            --build-arg BUILD_DATE="${buildDate}" \\
            --build-arg VCS_REF="${gitCommit}" \\
            --label "org.opencontainers.image.created=${buildDate}" \\
            --label "org.opencontainers.image.revision=${gitCommit}" \\
            --label "org.opencontainers.image.source=${gitRepo}" \\
            --label "org.opencontainers.image.title=${serviceName}" \\
            --label "com.northcare.service=${serviceName}" \\
            --label "com.northcare.build=${env.BUILD_NUMBER}" \\
            --cache-from "${image}" \\
            --progress=plain \\
            ${context}

        echo "✅ Docker build complete: ${image}"
    """
}
