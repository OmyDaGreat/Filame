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

## Tips

- Always export configs before pushing to GitHub
- Use descriptive commit messages
- Pull before making major changes to avoid conflicts
- Use ignore patterns for large or sensitive files
- Keep device names unique for easy identification
- Regularly backup your GitHub repository
