/**
 * northcareScan — runs Trivy vulnerability scan on the built Docker image.
 * Fails the build on CRITICAL CVEs; reports HIGH CVEs as warnings.
 *
 * @param config.image        Full image name+tag to scan
 * @param config.severity     Comma-separated severities that fail the build (default: CRITICAL)
 * @param config.exitCode     Trivy exit code on findings (1 = fail, 0 = warn-only)
 */
def call(Map config = [:]) {
    def image    = config.image    ?: error("northcareScan: 'image' required")
    def severity = config.severity ?: 'CRITICAL'
    def exitCode = config.exitCode ?: 1

    sh """
        set -euo pipefail

        echo "=== Trivy security scan: ${image} ==="

        # Pull Trivy if not available (idempotent)
        if ! command -v trivy &>/dev/null; then
            curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | \\
                sh -s -- -b /usr/local/bin
        fi

        # Update vulnerability database
        trivy image --download-db-only --quiet

        # Full scan — output to file for archiving
        trivy image \\
            --exit-code ${exitCode} \\
            --severity ${severity} \\
            --format table \\
            --output trivy-results.txt \\
            "${image}" || {
                echo "❌ Trivy found ${severity} vulnerabilities in ${image}"
                cat trivy-results.txt
                exit 1
            }

        # Also emit SARIF for GitHub Security tab (non-blocking)
        trivy image \\
            --exit-code 0 \\
            --severity HIGH,CRITICAL \\
            --format sarif \\
            --output trivy-results.sarif \\
            "${image}" || true

        echo "✅ Security scan passed for ${image}"
        cat trivy-results.txt
    """

    archiveArtifacts artifacts: 'trivy-results.*', allowEmptyArchive: true
}
