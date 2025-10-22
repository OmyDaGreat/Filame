# Filame AUR Package

This directory contains the files needed to publish Filame to the Arch User Repository (AUR).

## Files

- `PKGBUILD` - The build script for creating the Arch package
- `.SRCINFO` - Metadata file for the AUR (auto-generated from PKGBUILD)
- `.github/workflows/aur-publish.yml` - Automated publishing workflow

## Automated Publishing

The project uses GitHub Actions to automatically publish releases to the AUR. The workflow:

1. **Triggers on**:
   - Manual workflow dispatch (for new releases)
   - Git tags starting with `v*`

2. **Automated steps**:
   - Builds the shadow JAR
   - Calculates SHA256 checksums
   - Updates PKGBUILD and .SRCINFO with new version
   - Creates a GitHub release
   - Publishes to AUR automatically

### Required GitHub Secrets

Configure these secrets in your repository settings:

- `AUR_USERNAME` - Your AUR username
- `AUR_EMAIL` - Your email associated with AUR account
- `AUR_SSH_PRIVATE_KEY` - SSH private key for AUR authentication

### Creating a New Release

#### Option 1: Workflow Dispatch (Recommended)

1. Go to Actions → "Build and Publish to AUR"
2. Click "Run workflow"
3. Select version bump type (patch/minor/major)
4. Click "Run workflow"

The workflow will automatically:
- Bump the version
- Create a git tag
- Build and release
- Update AUR

#### Option 2: Manual Git Tag

```bash
git tag v1.0.1
git push origin v1.0.1
```

The workflow will trigger automatically and publish to AUR.

## Publishing to AUR

### Initial Setup

1. Create an AUR account at https://aur.archlinux.org/register

2. Set up SSH keys for AUR:
   ```bash
   ssh-keygen -f ~/.ssh/aur
   cat ~/.ssh/aur.pub  # Add this to your AUR account
   ```

3. Configure SSH for AUR:
   ```bash
   # Add to ~/.ssh/config
   Host aur.archlinux.org
     IdentityFile ~/.ssh/aur
     User aur
   ```

### Publishing Updates

1. Update the version in `PKGBUILD`:
   ```bash
   pkgver=X.Y.Z
   ```

2. Generate the `.SRCINFO` file:
   ```bash
   makepkg --printsrcinfo > .SRCINFO
   ```

3. Test the package locally:
   ```bash
   makepkg -si
   ```

4. Clone the AUR repository (first time only):
   ```bash
   git clone ssh://aur@aur.archlinux.org/filame.git aur-filame
   ```

5. Copy files and commit:
   ```bash
   cp PKGBUILD .SRCINFO aur-filame/
   cd aur-filame
   git add PKGBUILD .SRCINFO
   git commit -m "Update to version X.Y.Z"
   git push
   ```

## Testing the Package

Before publishing, always test the package:

```bash
# Clean build
makepkg -sci

# Test the installed application
filame
```

## Notes

- The package depends on `java-runtime>=21`
- The build uses Gradle to create a shadow JAR with all dependencies
- The package installs to `/usr/share/java/filame/` with a launcher in `/usr/bin/filame`
