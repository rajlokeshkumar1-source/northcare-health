/**
 * northcarePush — logs in to ECR and pushes the image.
 * Also pushes a :latest tag for developer convenience.
 *
 * @param config.image        Full image name+tag to push
 * @param config.latestImage  Optional :latest tag to also push (default: derived from image)
 * @param config.region       AWS region (default: us-east-1)
 */
def call(Map config = [:]) {
    def image       = config.image  ?: error("northcarePush: 'image' required")
    def region      = config.region ?: 'us-east-1'
    def registry    = image.split('/')[0]
    def latestImage = config.latestImage ?: image.replaceAll(/:[^:]+$/, ':latest')

    sh """
        set -euo pipefail

        echo "Authenticating to ECR registry: ${registry}"
        aws ecr get-login-password --region ${region} | \\
            docker login --username AWS --password-stdin ${registry}

        echo "Pushing versioned image: ${image}"
        docker push "${image}"

        echo "Pushing latest tag: ${latestImage}"
        docker tag "${image}" "${latestImage}"
        docker push "${latestImage}"

        echo "✅ Push complete"
    """
}
