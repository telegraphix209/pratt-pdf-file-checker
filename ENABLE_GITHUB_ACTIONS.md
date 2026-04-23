# Enable GitHub Actions - Step by Step Guide

## 🎯 What We Just Did

✅ Deleted and recreated the `v1.0.0` tag  
✅ Pushed it to GitHub (this should trigger the workflow)

---

## 📋 Step 1: Enable GitHub Actions

GitHub Actions may be disabled by default for new repositories. Here's how to enable it:

### **Via Web Browser**:

1. **Go to your repository settings**:
   ```
   https://github.com/telegraphix209/pratt-pdf-file-checker/settings/actions
   ```

2. **Under "Actions permissions"**, select:
   - ✅ **"Allow all actions and reusable workflows"**
   
   OR if you see different options:
   - ✅ **"Allow selected actions and reusable workflows"**
   - Make sure to allow:
     - `actions/*`
     - `softprops/*`

3. **Click "Save"**

### **Check if Actions are already enabled**:

1. Go to: https://github.com/telegraphix209/pratt-pdf-file-checker/actions
2. If you see a green banner saying "Actions are enabled", you're good!
3. If you see a button saying "Enable Actions", click it

---

## 🚀 Step 2: Check if Workflow is Running

After enabling Actions:

1. **Go to Actions tab**:
   ```
   https://github.com/telegraphix209/pratt-pdf-file-checker/actions
   ```

2. **Look for**:
   - Workflow named "Build Multi-Platform Installers"
   - Status should show as "Running" (yellow circle) or "Success" (green checkmark)

3. **Click on the workflow run** to see details:
   - You should see 3 jobs:
     - `build-macos` (running on macos-latest)
     - `build-windows` (running on windows-latest)
     - `build-linux` (running on ubuntu-latest)

---

## ⏱️ Step 3: Wait for Builds to Complete

**Expected build time**: 5-10 minutes

**What's happening**:
1. GitHub provisions 3 virtual machines (Mac, Windows, Linux)
2. Each VM:
   - Checks out your code
   - Installs Java 17
   - Builds the JAR file
   - Creates the native installer (DMG/EXE/DEB/RPM)
   - Uploads to the release

---

## 📦 Step 4: Verify Installers are Uploaded

Once the workflow completes:

1. **Go to the release page**:
   ```
   https://github.com/telegraphix209/pratt-pdf-file-checker/releases/tag/v1.0.0
   ```

2. **You should see 4 files under "Assets"**:
   - `Pratt-PDF-Checker-1.0.0.dmg` (macOS) ~60-150 MB
   - `Pratt-PDF-Checker-1.0.0.exe` (Windows) ~100-150 MB
   - `pratt-pdf-checker_1.0.0-1_amd64.deb` (Linux Debian/Ubuntu) ~100-150 MB
   - `pratt-pdf-checker_1.0.0-1.x86_64.rpm` (Linux RedHat/Fedora) ~100-150 MB

---

## 🔧 Troubleshooting

### **Issue: No workflow runs appearing**

**Solution 1**: Check if Actions are enabled
- Go to Settings → Actions
- Make sure "Allow all actions" is selected

**Solution 2**: Manually trigger the workflow
1. Go to: https://github.com/telegraphix209/pratt-pdf-file-checker/actions
2. Click on "Build Multi-Platform Installers" workflow
3. Click "Run workflow" button (dropdown)
4. Select branch: `main`
5. Click "Run workflow"

**Solution 3**: Check workflow file exists
- The file should be at: `.github/workflows/build-installers.yml`
- Verify at: https://github.com/telegraphix209/pratt-pdf-file-checker/blob/main/.github/workflows/build-installers.yml

### **Issue: Workflow runs but fails**

**Check the logs**:
1. Click on the failed workflow run
2. Click on the failed job (e.g., "build-macos")
3. Expand the failed step
4. Look for error messages

**Common errors**:
- `mvn: command not found` → Maven not installed on runner (should be pre-installed)
- `jpackage: command not found` → JDK not properly set up
- `No files found` → Build script didn't create the installer

### **Issue: Workflow succeeds but no assets uploaded**

**Check**:
1. Look at the "Upload to GitHub Releases" step in logs
2. Verify `GITHUB_TOKEN` is available (it should be automatic)
3. Check if the release exists: https://github.com/telegraphix209/pratt-pdf-file-checker/releases

---

## 🎉 Success!

Once you see all 4 installers on the release page, you're done! Users can now:

- **macOS**: Download the DMG, drag to Applications
- **Windows**: Download the EXE, run installer
- **Linux**: Download DEB or RPM, install with package manager

All with **zero dependencies** - Java is bundled!

---

## 📝 Next Steps (Optional)

### **Create a README badge**:

Add this to your README.md to show build status:

```markdown
[![Build Status](https://github.com/telegraphix209/pratt-pdf-file-checker/actions/workflows/build-installers.yml/badge.svg)](https://github.com/telegraphix209/pratt-pdf-file-checker/actions/workflows/build-installers.yml)
```

### **Set up automatic releases**:

Every time you push a tag like `v1.0.1`, `v1.1.0`, etc., the workflow will automatically:
- Build all platforms
- Create a GitHub Release
- Upload all installers

Just run:
```bash
git tag v1.0.1
git push origin v1.0.1
```

---

**Need help?** Check the workflow logs or let me know what error you're seeing!
