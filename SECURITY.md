# Security Policy — Daf Note App (نت‌نگار دف)

## Reporting a Vulnerability

If you discover a security vulnerability in Daf Note App, please do not open a public issue. Instead, report it privately to the repository maintainers or email the project team directly with details and steps to reproduce.

---

## Security Advisory: Signing Key Exposure & Keystore Rotation

### 1. Incident Summary
The repository review identified that a debug keystore (`debug.keystore.base64`) with default credentials (alias `androiddebugkey`, password `android`, SHA-256: `EA:38:63:9B:E7:8D:94:FE:CD:EC:CA:A1:B6:C1:E4:F8:7A:A0:F7:7D:0F:C8:ED:5B:6E:6F:DB:5D:C0:2F:8A:1D`) was previously present in git commits.

Any key material committed to version control must be considered publicly compromised. It must never be reused for production distribution.

### 2. Upstream Keystore Generation & Rotation

To establish an uncompromised signing identity for release builds:

1. Generate a new, private release keystore locally:
   ```bash
   keytool -genkeypair -v \
     -keystore release.keystore \
     -alias daf-release-key \
     -keyalg RSA \
     -keysize 4096 \
     -validity 10000 \
     -storetype PKCS12
   ```

2. Extract and record the certificate SHA-256 fingerprint:
   ```bash
   keytool -list -v -keystore release.keystore -alias daf-release-key
   ```

3. Encode the binary keystore as a single-line Base64 string:
   ```bash
   base64 -w 0 release.keystore > release.keystore.base64
   ```

4. In the GitHub repository settings (**Settings > Secrets and variables > Actions**):
   - Add **Repository Secrets**:
     - `RELEASE_KEYSTORE_BASE64`: Content of `release.keystore.base64`
     - `STORE_PASSWORD`: Keystore password chosen during generation
     - `KEY_ALIAS`: `daf-release-key` (or your chosen alias)
     - `KEY_PASSWORD`: Key password chosen during generation
   - Add **Repository Variable**:
     - `EXPECTED_CERT_SHA256`: Hexadecimal SHA-256 fingerprint (lowercase, no colons)

5. Store your offline backup of `release.keystore` in a secure password manager or hardware vault. Never commit it to git.

### 3. Git History Cleansing with `git-filter-repo`

To purge compromised key files from Git commit history:

```bash
# 1. Install git-filter-repo (Python 3 required)
pip install git-filter-repo

# 2. In a fresh, bare mirror clone:
git clone --mirror https://github.com/XxSirFunxX/Daf-note-app.git daf-clean
cd daf-clean

# 3. Strip the compromised files from all commits and tags:
git filter-repo --invert-paths \
  --path debug.keystore \
  --path debug.keystore.base64 \
  --path app/debug.keystore \
  --path app/debug.keystore.base64

# 4. Review log to ensure commits are clean:
git log --stat

# 5. Push rewritten history (Requires admin approval):
# WARNING: Force-pushing rewrites commit SHAs!
# git push origin --force --all
# git push origin --force --tags
```

> **Warning regarding history rewriting:**
> Force-pushing after `git-filter-repo` will rewrite commit hashes across the entire repository. Anyone with an existing local clone will experience divergent histories and will need to re-clone the repository. Open Pull Requests may also need to be rebased.

### 4. Impact on Existing Installed Applications (In-Place Updates)

Android enforces certificate matching (`Signature` validation) during app updates:
- An APK signed with a new keystore **cannot** be installed as an in-place update over an app signed with the previous key. The Android Package Manager will abort the installation with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- Users updating to an APK signed with a new key will need to uninstall the old version first (or backup/restore their projects using `.dafnt` export files).

---

## Best Practices Implemented in CI/CD
- Keystore files are strictly ignored in `.gitignore`.
- GitHub Actions workflow decodes keystores into `$RUNNER_TEMP` (outside the repository checkout) and scrubs temporary files immediately after signing.
- Missing credentials cause builds to fail fast; CI never falls back to uncommitted or hardcoded secrets.
- Verification checks that release APKs are not debuggable (`android:debuggable="false"`).
