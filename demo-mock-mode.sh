#!/bin/bash
# Demo script to show mock mode and two-way sync functionality
# Note: This demo script is Unix/Linux specific (uses /tmp)
# For cross-platform demo, use the actual Filame application with mock mode enabled

echo "=== Filame Mock Mode Demo ==="
echo ""
echo "This demo shows:"
echo "1. Mock mode for non-Linux environments"
echo "2. Two-way sync of package configurations"
echo ""

# Create a temporary config directory for demo
DEMO_DIR="/tmp/filame-demo-$$"
mkdir -p "$DEMO_DIR/.config/filame"
export HOME="$DEMO_DIR"

echo "=== Step 1: Creating demo configuration ==="
cat > "$DEMO_DIR/.config/filame/config.yaml" << 'EOF'
deviceName: demo-device
githubRepo: https://github.com/demo/dotfiles.git
mockMode: true
packageBundles:
  - name: vim
    source: official
    description: Text editor
    configFiles:
      - sourcePath: /home/user/.vimrc
        destinationPath: vim/.vimrc
        description: Vim configuration
  - name: i3
    source: official
    description: Tiling window manager
    configFiles:
      - sourcePath: /home/user/.config/i3/config
        destinationPath: i3/config
        description: Main i3 config
EOF

echo "✓ Created config with mock mode enabled"
cat "$DEMO_DIR/.config/filame/config.yaml"
echo ""

echo "=== Step 2: Creating mock repository structure ==="
mkdir -p "$DEMO_DIR/.config/filame/repo/vim"
mkdir -p "$DEMO_DIR/.config/filame/repo/i3"

# Create package metadata files
cat > "$DEMO_DIR/.config/filame/repo/vim/package.yaml" << 'EOF'
name: vim
source: official
description: Text editor
configFiles:
  - sourcePath: /home/user/.vimrc
    destinationPath: vim/.vimrc
    description: Vim configuration
EOF

cat > "$DEMO_DIR/.config/filame/repo/i3/package.yaml" << 'EOF'
name: i3
source: official
description: Tiling window manager
configFiles:
  - sourcePath: /home/user/.config/i3/config
    destinationPath: i3/config
    description: Main i3 config
EOF

echo "✓ Created package metadata files:"
echo "  - vim/package.yaml"
echo "  - i3/package.yaml"
echo ""

echo "=== Step 3: Demonstrating package metadata export ==="
echo "When a package bundle is added or edited, metadata is automatically"
echo "exported to the repository as package.yaml files."
echo ""
echo "Example package.yaml content:"
cat "$DEMO_DIR/.config/filame/repo/vim/package.yaml"
echo ""

echo "=== Step 4: Mock mode features ==="
echo "✓ Mock mode is enabled (mockMode: true)"
echo "✓ Package operations will be simulated"
echo "✓ Git operations work normally"
echo "✓ Configuration management works normally"
echo "✓ Metadata export/import works normally"
echo ""

echo "=== Step 5: Testing mock mode detection ==="
echo "OS detected: $(uname -s)"
if [[ "$(uname -s)" != "Linux" ]]; then
    echo "✓ Non-Linux system detected - mock mode would auto-enable"
else
    echo "✓ Linux system detected - mock mode is optional"
fi
echo ""

echo "=== Demo Complete ==="
echo "Mock mode allows Filame to:"
echo "  • Run on macOS, Windows, and other non-Linux systems"
echo "  • Work in CI/CD pipelines"
echo "  • Test application logic without affecting system packages"
echo ""
echo "Two-way sync ensures:"
echo "  • Package metadata is exported when bundles are added/edited"
echo "  • Package metadata is imported when scanning the repository"
echo "  • Seamless synchronization across multiple devices"
echo ""

# Cleanup
rm -rf "$DEMO_DIR"
echo "Cleaned up demo files"
