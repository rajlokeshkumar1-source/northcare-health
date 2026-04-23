/**
 * northcareGitOpsUpdate — clones gitops-argocd, patches the image tag in
 * <serviceName>/dev/values.yaml, and pushes the commit back.
 *
 * ArgoCD detects the commit and rolls out the new image automatically.
 *
 * @param config.serviceName  Service directory name in gitops-argocd (e.g. 'hospital-core')
 * @param config.imageTag     Full image tag written into values.yaml (e.g. '42-abc1234')
 */
def call(Map config = [:]) {
    def serviceName  = config.serviceName ?: error("northcareGitOpsUpdate: 'serviceName' required")
    def imageTag     = config.imageTag    ?: error("northcareGitOpsUpdate: 'imageTag' required")
    def gitopsRepo   = config.gitopsRepo  ?: "northcare-health/gitops-argocd"
    def targetBranch = config.branch      ?: "main"

    withCredentials([
        usernamePassword(
            credentialsId: 'github-creds',
            usernameVariable: 'GH_USER',
            passwordVariable: 'GH_TOKEN'
        )
    ]) {
        sh """
            set -euo pipefail

            WORKDIR="\$(mktemp -d -p "\${WORKSPACE}")"
            trap 'rm -rf "\${WORKDIR}"' EXIT

            echo "Cloning gitops-argocd..."
            git clone --depth=1 --branch=${targetBranch} \\
                https://\${GH_USER}:\${GH_TOKEN}@github.com/${gitopsRepo}.git "\${WORKDIR}"

            cd "\${WORKDIR}"

            VALUES_FILE="${serviceName}/dev/values.yaml"
            if [ ! -f "\${VALUES_FILE}" ]; then
                echo "ERROR: \${VALUES_FILE} not found in gitops repo" >&2
                exit 1
            fi

            # Patch the image tag — only the line containing 'tag:' under the image stanza
            sed -i "s|^  tag:.*|  tag: \\"${imageTag}\\"|" "\${VALUES_FILE}"

            # Verify the change was applied
            grep "tag:" "\${VALUES_FILE}"

            git config user.email "jenkins@northcare-health.local"
            git config user.name "Jenkins CI"
            git add "\${VALUES_FILE}"

            if git diff --staged --quiet; then
                echo "No changes to commit — image tag may already be ${imageTag}"
                exit 0
            fi

            git commit -m "deploy(${serviceName}): ${imageTag} [skip ci]"
            git push origin ${targetBranch}

            echo "✅ GitOps update committed for ${serviceName}:${imageTag}"
        """
    }
}
