/**
 * northcareTest — runs lint and unit tests for a NorthCare Python microservice.
 * Expects a virtualenv-compatible project with pyproject.toml or requirements*.txt.
 *
 * @param config.serviceName  Service name (for display)
 * @param config.testDir      Directory containing tests (default: 'tests/')
 * @param config.coverageMin  Minimum coverage percentage to fail the build (default: 80)
 */
def call(Map config = [:]) {
    def serviceName = config.serviceName ?: env.SERVICE_NAME ?: 'service'
    def testDir     = config.testDir     ?: 'tests/'
    def coverageMin = config.coverageMin ?: 80

    sh """
        set -euo pipefail

        echo "=== Setting up Python environment for ${serviceName} ==="
        python3 -m venv .venv
        . .venv/bin/activate

        pip install --quiet --upgrade pip

        if [ -f pyproject.toml ]; then
            pip install --quiet ".[dev,test]"
        elif [ -f requirements-dev.txt ]; then
            pip install --quiet -r requirements.txt -r requirements-dev.txt
        elif [ -f requirements.txt ]; then
            pip install --quiet -r requirements.txt pytest pytest-cov flake8
        else
            pip install --quiet pytest pytest-cov flake8
        fi

        echo "=== Linting (flake8) ==="
        flake8 . --max-line-length=120 --exclude=.venv,migrations --format=default || {
            echo "⚠️  Lint warnings found (non-blocking for now)"
        }

        echo "=== Running tests with coverage ==="
        pytest ${testDir} \\
            --junitxml=test-results/results.xml \\
            --cov=. \\
            --cov-report=xml:coverage/coverage.xml \\
            --cov-report=html:coverage \\
            --cov-fail-under=${coverageMin} \\
            -v

        echo "✅ Tests passed for ${serviceName}"
    """
}
