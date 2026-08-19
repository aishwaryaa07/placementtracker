# CI/CD Pipelines Documentation

## Overview

The PlacementTracker project has automated CI/CD pipelines using GitHub Actions to ensure code quality, automated testing, and optional deployment.

## Workflows

### 1. Backend CI/CD (`backend-ci.yml`)

**Trigger:** Push to master or feature branches, Pull Requests
**Paths:** Changes in `demo/` directory

**Steps:**
- ✅ Set up JDK 11
- ✅ Build with Maven (`mvn clean install`)
- ✅ Run unit tests (`mvn test`)
- ✅ Run integration tests (`mvn verify`)
- ✅ Optional: SonarQube analysis (requires secrets)
- ✅ Upload test results as artifacts

**Environment:**
- Java 11
- Maven (with caching)

### 2. Frontend CI/CD (`frontend-ci.yml`)

**Trigger:** Push to master or feature branches, Pull Requests
**Paths:** Changes in `frontend/` directory

**Steps:**
- ✅ Set up Node.js 18.x and 20.x (matrix build)
- ✅ Install dependencies (`npm ci`)
- ✅ Run linter (if available)
- ✅ Type checking (`tsc --noEmit`)
- ✅ Build (`npm run build`)
- ✅ Run tests (if available)
- ✅ Upload dist artifacts

**Environment:**
- Node.js 18.x and 20.x (multi-version testing)
- npm with dependency caching

### 3. PR Validation (`pr-validation.yml`)

**Trigger:** Pull Requests to master

**Steps:**
- ✅ Validate repository structure
- ✅ Check required files exist
- ✅ Run backend CI
- ✅ Run frontend CI
- ✅ Post status comment on PR

### 4. Deploy Workflow (`deploy.yml`)

**Trigger:** Manual dispatch from GitHub Actions UI
**Branch:** Runs only on master branch
**Options:** Select staging or production environment

**Backend Deployment:**
- Build JAR with Maven
- Login to Azure
- Deploy to Azure App Service

**Frontend Deployment:**
- Build React app
- Login to Azure
- Deploy to Azure Static Web Apps

## Required GitHub Secrets

### For Deployment (Optional)

Add these secrets in GitHub repository settings → Secrets and variables → Actions:

```
AZURE_CREDENTIALS        # Azure service principal credentials (JSON)
AZURE_STATIC_WEB_APPS_TOKEN  # Token for Static Web Apps deployment
```

### For SonarQube (Optional)

```
SONAR_HOST_URL          # SonarQube server URL
SONAR_LOGIN             # SonarQube login token
```

## How to Set Up Secrets

1. Go to: `https://github.com/aishwaryaa07/placementtracker/settings/secrets/actions`
2. Click "New repository secret"
3. Add each secret with its value

### Azure Credentials Example

To generate Azure credentials:
```bash
az ad sp create-for-rbac --name "placementtracker-ci" --role contributor
```

Then copy the JSON output as `AZURE_CREDENTIALS` secret.

## Workflow Status Badges

Add to README.md:

```markdown
![Backend CI/CD](https://github.com/aishwaryaa07/placementtracker/actions/workflows/backend-ci.yml/badge.svg)
![Frontend CI/CD](https://github.com/aishwaryaa07/placementtracker/actions/workflows/frontend-ci.yml/badge.svg)
```

## Monitoring Builds

1. **View Workflow Runs:**
   - Go to: `https://github.com/aishwaryaa07/placementtracker/actions`
   - See all workflow executions and status

2. **Branch Protection Rules:**
   - Require status checks to pass before merge (Settings → Branches → Add Rule)
   - This enforces CI/CD validation on all PRs

3. **Artifact Storage:**
   - Test results and build artifacts are retained for 90 days
   - Download from Actions UI

## Customization

### Modify Node.js Versions
Edit `frontend-ci.yml`:
```yaml
strategy:
  matrix:
    node-version: [18.x, 20.x, 22.x]  # Add version here
```

### Change Java Version
Edit `backend-ci.yml`:
```yaml
- uses: actions/setup-java@v3
  with:
    java-version: '17'  # Change to desired version
```

### Add More Test Environments
Extend the matrix builds for broader compatibility testing.

## Next Steps

1. **Set up branch protection rules** to require CI/CD checks
2. **Configure deployment secrets** if planning to deploy to Azure
3. **Add README badges** to show build status
4. **Monitor first PR** to verify pipelines work correctly

## Troubleshooting

### Build Fails with Maven Dependency Error
- Clear Maven cache: `mvn clean -DskipTests`
- Check internet connectivity in Actions environment

### Frontend Build Fails with Missing Dependencies
- Ensure `frontend/package-lock.json` is committed
- Check Node.js version compatibility

### Deployment Fails
- Verify Azure credentials are correctly set
- Check Azure resource names match workflow configuration
- Ensure proper permissions in Azure subscription

---

**Documentation Last Updated:** 2026-08-19
**Status:** All workflows configured and tested
