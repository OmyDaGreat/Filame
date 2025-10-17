# Filame Usage Example

## Directory Structure

After setting up Filame, your configuration will be organized as follows:

```
~/.config/filame/
├── config.yaml          # Filame configuration file
└── repo/                # Local Git repository
    ├── .git/            # Git metadata
    ├── i3/
    │   └── config       # i3 window manager config
    ├── polybar/
    │   └── config       # Polybar status bar config
    ├── bashrc           # Bash configuration
    ├── zshrc            # Zsh configuration
    └── ...              # Other tracked config files
```

## Example Workflow

### 1. Initial Setup on Device 1 (Laptop)

```bash
# Start Filame
./gradlew run

# Configure settings (Option 1)
Device name: arch-laptop
GitHub repo: https://github.com/username/dotfiles.git

# Add configuration files (Option 2)
Source: ~/.config/i3/config → Destination: i3/config
Source: ~/.bashrc → Destination: bashrc
Source: ~/.vimrc → Destination: vimrc

# Export configs to repo (Option 4)
✓ Exported 3 configuration files

# Sync to GitHub (Option 7)
Enter commit message: Initial dotfiles from laptop
✓ Successfully pushed changes to GitHub
```

### 2. Setup on Device 2 (Desktop)

```bash
# Start Filame
./gradlew run

# Configure settings (Option 1)
Device name: arch-desktop
GitHub repo: https://github.com/username/dotfiles.git

# Sync from GitHub (Option 6)
✓ Successfully pulled changes from GitHub

# Import configs (Option 5)
✓ Imported 3 configuration files
```

### 3. Making Changes

When you modify a config file on any device:

```bash
# Export the changed files (Option 4)
✓ Exported 3 configuration files

# Sync to GitHub (Option 7)
Enter commit message: Updated i3 config with new keybindings
✓ Successfully pushed changes to GitHub
```

On other devices:

```bash
# Pull latest changes (Option 6)
✓ Successfully pulled changes from GitHub

# Import configs (Option 5)
✓ Imported 3 configuration files
```

## Configuration File Example

The `~/.config/filame/config.yaml` file contains:

```yaml
deviceName: arch-laptop
githubRepo: https://github.com/username/dotfiles.git
configFiles:
  - sourcePath: /home/user/.config/i3/config
    destinationPath: i3/config
    description: i3 window manager configuration
  - sourcePath: /home/user/.bashrc
    destinationPath: bashrc
    description: Bash shell configuration
  - sourcePath: /home/user/.vimrc
    destinationPath: vimrc
    description: Vim editor configuration
packages:
  - name: vim
    source: official
    description: Text editor
  - name: i3-wm
    source: official
    description: Tiling window manager
  - name: yay
    source: aur
    description: AUR helper
  - name: spotify
    source: aur
    description: Music streaming client
ignorePatterns:
  - "*.log"
  - "*.tmp"
  - ".cache/*"
  - "*.lock"
```

## Common Use Cases

### Tracking Window Manager Configs

```
i3: ~/.config/i3/config → i3/config
awesome: ~/.config/awesome/rc.lua → awesome/rc.lua
bspwm: ~/.config/bspwm/bspwmrc → bspwm/bspwmrc
```

### Tracking Shell Configs

```
bash: ~/.bashrc → bashrc
zsh: ~/.zshrc → zshrc
fish: ~/.config/fish/config.fish → fish/config.fish
```

### Tracking Editor Configs

```
vim: ~/.vimrc → vimrc
neovim: ~/.config/nvim/init.vim → nvim/init.vim
emacs: ~/.emacs.d/init.el → emacs/init.el
```

### Tracking Terminal Configs

```
alacritty: ~/.config/alacritty/alacritty.yml → alacritty/alacritty.yml
kitty: ~/.config/kitty/kitty.conf → kitty/kitty.conf
terminator: ~/.config/terminator/config → terminator/config
```

## Package Management

### Adding Packages to Track

From the Manage Packages menu (option 9):

1. Add official packages:
   ```
   Package name: vim
   Source: official
   Description: Text editor
   ```

2. Add AUR packages:
   ```
   Package name: spotify
   Source: aur
   Description: Music streaming
   ```

### Installing Paru

If paru is not installed and you try to install an AUR package, Filame will prompt you to install it:

```bash
# From the package management menu:
1. Install paru (if not installed)
# This will automatically:
# - Install dependencies (git, base-devel)
# - Clone paru from AUR
# - Build and install paru
```

### Installing All Tracked Packages

```bash
# From the package management menu:
4. Install missing packages
# This will install all tracked packages that aren't already on the system
```

### Complete Setup Example

When setting up a new Arch system:

1. Configure Filame with your GitHub repo
2. Pull configuration from GitHub
3. Install paru (option 9 → 1)
4. Install all missing packages (option 9 → 4)
5. Import configs (option 5)

Your system is now configured with all your dotfiles and packages!

## Tips

- Always export configs before pushing to GitHub
- Use descriptive commit messages
- Pull before making major changes to avoid conflicts
- Use ignore patterns for large or sensitive files
- Keep device names unique for easy identification
- Regularly backup your GitHub repository
