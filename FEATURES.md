# Filame Features

## Core Functionality

### 1. Configuration Management
- **Track unlimited config files** - Add any file from your system to be tracked
- **Source and destination paths** - Map local files to repository structure
- **File descriptions** - Add optional descriptions to each tracked file
- **Path expansion** - Automatic expansion of `~` to home directory

### 2. Device Management
- **Multiple device support** - Manage configs across different machines
- **Unique device names** - Identify which device made changes
- **Device-specific settings** - Each device maintains its own config

### 3. GitHub Integration
- **Repository initialization** - Automatically clone or initialize Git repo
- **Pull operations** - Sync latest changes from GitHub
- **Push operations** - Upload local changes to GitHub
- **Commit messages** - Custom or auto-generated commit messages
- **Git authentication** - Support for SSH keys and HTTPS tokens

### 4. File Synchronization
- **Export configs** - Copy tracked files from system to repository
- **Import configs** - Apply repository files to system
- **Bidirectional sync** - Move files in either direction
- **Safe copying** - Uses `StandardCopyOption.REPLACE_EXISTING`
- **Directory creation** - Automatically creates parent directories

### 5. Ignore Patterns
- **Pattern management** - Add or remove ignore patterns
- **Glob support** - Standard glob patterns (*.log, .cache/*, etc.)
- **Default patterns** - Pre-configured common patterns
- **Custom patterns** - Add your own patterns for specific needs

### 6. Terminal User Interface
- **Kotter-powered UI** - Beautiful terminal interface
- **Color-coded output** - Visual feedback with colors
  - Cyan: Headers and titles
  - Green: Success messages and menu options
  - Yellow: Warnings and notes
  - Red: Errors
  - White: Normal text
- **Box drawing** - Attractive menu borders
- **Clear navigation** - Numbered menu options
- **Interactive prompts** - User-friendly input handling

### 7. Package Management
- **Track packages** - Add official and AUR packages to your configuration
- **Paru integration** - Auto-install paru AUR helper when needed
- **Package search** - Search both official repos and AUR
- **Installation status** - Track which packages are installed
- **Batch install** - Install all tracked packages at once
- **System updates** - Update all official and AUR packages
- **Package removal** - Uninstall packages from system
- **Source tracking** - Distinguish between official and AUR packages

### 8. Configuration Storage
- **YAML format** - Human-readable configuration
- **Location**: `~/.config/filame/config.yaml`
- **Auto-save** - Configuration saved after each change
- **Serialization** - Kotlinx Serialization for type-safe config

### 9. Error Handling
- **Result types** - Functional error handling with Result<T>
- **Descriptive errors** - Clear error messages
- **Graceful degradation** - Skip non-existent files
- **Exception handling** - Catches and reports errors

## Technical Architecture

### Components

1. **Runner.kt** - Main application and UI logic
   - Menu system
   - User input handling
   - Session management

2. **Config.kt** - Configuration data structures
   - FilameConfig data class
   - ConfigFile data class
   - Package data class
   - ConfigManager singleton
   - Serialization support

3. **GitManager.kt** - Git operations
   - Repository initialization
   - Pull/Push operations
   - Commit handling
   - File export/import

4. **PackageManager.kt** - Package management
   - Paru installation and detection
   - Package search (official + AUR)
   - Package installation/removal
   - System updates
   - Installation status tracking

### Dependencies

- **Kotter 1.1.2** - Terminal UI framework
- **JGit 6.10.0.202406032230-r** - Git operations
- **Kotlinx Serialization 1.7.3** - Config serialization
- **KAML 0.61.0** - YAML parsing
- **Kotlinx Coroutines 1.8.1** - Async operations

### Design Patterns

- **Singleton** - ConfigManager for global config access
- **Data Classes** - Immutable configuration structures
- **Result Type** - Functional error handling
- **Separation of Concerns** - UI, Logic, and Data separated

## Future Enhancements

Potential features for future versions:

1. **Encryption** - Encrypt sensitive configuration files
2. **Backup System** - Automatic backups before applying changes
3. **Diff Viewer** - Show differences before importing
4. **Templates** - Config file templates for common setups
5. **Multiple Repos** - Support for multiple GitHub repositories
6. **File Browser** - Interactive file selection
7. **Profiles** - Multiple configuration profiles
8. **Conflict Resolution** - Interactive merge conflict handling
9. **Auto-sync** - Automatic periodic syncing
10. **Webhooks** - Trigger sync on remote changes
11. **Package Groups** - Organize packages into categories
12. **Dependency Management** - Track package dependencies

## Platform Support

- **Primary**: Arch Linux
- **Compatible**: Any Linux distribution
- **Requirements**: Java 21+, Git

## Security Considerations

- Configuration files stored in `~/.config/filame/`
- Git credentials managed by system Git
- No plaintext password storage
- SSH key support recommended
- Option to exclude sensitive files via ignore patterns
