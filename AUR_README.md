# Filame AUR Package

This directory contains the files needed to publish Filame to the Arch User Repository (AUR).

## Files

- `PKGBUILD` - The build script for creating the Arch package
- `.SRCINFO` - Metadata file for the AUR (auto-generated from PKGBUILD)

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
