# Mock Mode Guide

## Overview

Mock mode allows Filame to run in non-Linux environments (such as macOS, Windows, or CI/CD pipelines) where actual package management operations cannot be performed. This is useful for:

- Testing the application logic
- Developing on non-Linux systems
- CI/CD environments
- Demonstrating the application

## Features

When mock mode is enabled:

- **Package installation** - Simulated (displays what would be installed)
- **Package removal** - Simulated (displays what would be removed)
- **Package updates** - Simulated (displays update operations)
- **Paru installation** - Simulated (assumes paru is available)
- **Package status** - Always returns "not installed" to simulate fresh system
- **Configuration file operations** - Still functional (can copy files)
- **Git operations** - Fully functional
- **Package metadata export** - Fully functional

## Enabling Mock Mode

### Automatic Detection

Filame automatically detects non-Linux systems and enables mock mode on first run:

```bash
./gradlew run
```

Output:
```
Non-Linux system detected. Enabling mock mode for package operations.
```

### Manual Configuration

You can manually enable or disable mock mode:

1. Run Filame
2. Select "1. Configure settings"
3. When prompted "Enable mock mode for non-Linux environments? (y/n):", enter:
   - `y` to enable mock mode
   - `n` to disable mock mode

### Configuration File

Mock mode is stored in `~/.config/filame/config.yaml`:

```yaml
deviceName: my-device
githubRepo: https://github.com/username/dotfiles.git
mockMode: true
packageBundles:
  - name: vim
    source: official
    description: Text editor
```

## Mock Mode Behavior

### Package Installation

**Real Mode:**
```
Installing vim...
[sudo] password for user:
resolving dependencies...
looking for conflicting packages...
Packages (1) vim-9.0.1831-1
Total Installed Size:  38.58 MiB
```

**Mock Mode:**
```
[MOCK] Would install package: vim from official
✓ Package 'vim' installed successfully!
```

### Package Status

In mock mode, all packages are shown as not installed, allowing you to test the installation flow:

```
Package Bundles

1. [✗] vim (official)
   Text editor
   Config files: 1
     • vim/.vimrc

2. [✗] i3 (official)
   Tiling window manager
   Config files: 2
     • i3/config
     • i3/status.conf
```

## Two-Way Configuration Sync

Mock mode fully supports the two-way sync feature:

### From Local to Repo (Export)

When you add or edit a package bundle, Filame automatically:
1. Saves the bundle to local config (`~/.config/filame/config.yaml`)
2. Exports package metadata to the repo (`<package-name>/package.yaml`)

Example:
```
✓ Package bundle added successfully!
✓ Package metadata exported to repo: vim/package.yaml
```

### From Repo to Local (Import)

When you scan the repository:
1. Filame reads all package directories
2. Loads `package.yaml` files if they exist
3. Updates local config with discovered packages

Example:
```
✓ Found 3 package bundle(s)
  • vim (official) - 1 config file(s)
  • i3 (official) - 2 config file(s)
  • spotify (aur) - 0 config file(s)
```

## Use Cases

### 1. Development on macOS/Windows

```bash
# Clone the repo
git clone https://github.com/OmyDaGreat/Filame.git
cd Filame

# Build and run (mock mode auto-enabled)
./gradlew run
```

### 2. CI/CD Testing

```yaml
# .github/workflows/test.yml
name: Test
on: [push]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Test Filame
        run: |
          ./gradlew build
          ./gradlew test
```

### 3. Demo Mode

Mock mode is perfect for demonstrations where you want to show the application flow without actually modifying the system.

## Limitations

In mock mode, you cannot:

- Actually install packages on the system
- Actually remove packages from the system
- Check if packages are truly installed (always returns false)
- Use real package search (pacman/paru commands won't work)

However, all configuration management, Git operations, and metadata export/import work normally.

## Disabling Mock Mode

To switch back to real mode on a Linux system:

1. Run Filame
2. Select "1. Configure settings"
3. When prompted about mock mode, enter `n`
4. Package operations will now use real pacman/paru commands

## Testing Mock Mode

Run the test suite to verify mock mode functionality:

```bash
./gradlew test
```

Tests include:
- Mock mode configuration
- Package installation in mock mode
- Paru installation in mock mode
- Package metadata export
- Two-way sync operations
