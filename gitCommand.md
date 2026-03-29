<!-- spellcheck: off -->

# Git Command Reference

## ⚠️ Detached HEAD State

### Problem

🔴 If you commit in detached HEAD state, commits won't belong to any branch and may be lost.

### Solutions

#### Option 1: Return to existing branch (recommended)

```bash
git checkout main
# or
git checkout master
```

Then update:

```bash
git pull
```

#### Option 2: Create new branch from current commit

If you want to keep work from this commit point:

```bash
git checkout -b my-feature-branch
```

#### Option 3: List all available branches

```bash
git branch -a
```

### How This Happens

Generally occurs after:

```bash
git checkout <commit_hash>
```

---

## Essential Git Commands

### 📁 Initialization & Configuration

```bash
git init                           # Initialize a repository
git clone <url>                   # Clone a repository
git config --global user.name "Your Name"
git config --global user.email "your@email.com"
```

### 🔍 View Status & History

```bash
git status                         # Show current state
git log                           # Commit history
git log --oneline                 # Abbreviated history
git diff                          # Show changes
```

### ➕ Stage & Commit Changes

```bash
git add .                         # Stage all files
git add file.txt                  # Stage specific file
git commit -m "message"           # Create commit
```

### 🌿 Branch Management

```bash
git branch                        # List branches
git branch new-branch             # Create branch
git checkout new-branch           # Switch branch
git checkout -b new-branch        # Create and switch
```

### 🔄 Remote Operations

```bash
git pull                          # Fetch and update
git fetch                         # Fetch without applying
git push                          # Push commits
git push origin main              # Push to specific branch
git push origin HEAD:main         # Push HEAD to remote branch
```

### 🔀 Merge & Rebase

```bash
git merge other-branch            # Merge branches
git rebase other-branch           # Reorganize history
```

### ⏪ Undo Changes

```bash
git restore file.txt                    # Discard file changes
git reset --soft HEAD~1                 # Undo commit, keep changes
git reset --hard HEAD~1                 # Undo commit completely ⚠️
git checkout -- file.txt                # Restore old version
```

### 🔗 Remote Management

```bash
git remote -v                     # List remote repositories
git remote add origin <url>       # Add remote repository
```

---

## ✅ Standard Workflow

The typical daily workflow:

```bash
# Make changes to files
git add .
git commit -m "Descriptive message"
git push
```

Or to push to a specific branch:

```bash
git push origin feature-branch
```
