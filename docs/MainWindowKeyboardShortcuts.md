# Main Window Keyboard Shortcuts (GHCU)

---

## Menu Shortcuts

### File Menu

| Shortcut | Action | Notes |
|---------|--------|------|
| **Shortcut + O** | Open working directory | Opens the directory chooser to set the working directory. |
| **Shortcut + Q** | Quit | Exits the application. |

---

### Edit Menu

| Shortcut | Action | Notes |
|---------|--------|------|
| **Shortcut + G** | Grade Reports | Opens the grading window for the selected assignment. |
| **Shortcut + I** | Add Ignored File | Prompts for a filename to append to the ignored file list. |
| **Shortcut + S** | Save Assignments | Saves assignments to disk (`assignments.json`). |
| **Shortcut + L** | Load Configuration | Loads a configuration file that points to the ignored list path. |

---

### Process Menu

| Shortcut | Action | Notes |
|---------|--------|------|
| **Shortcut + P** | Pull Repositories | Runs the GitHub Classroom pull/clone command. |
| **Shortcut + E** | Extract | Extracts student packages into the consolidated submissions folder. |
| **Shortcut + I** | Imports | Generates `imports.txt` in the submissions folder. |
| **Shortcut + R** | Grading Reports | Generates feedback reports for the selected assignment. |
| **Shortcut + A** | Run All | Runs the full pipeline (pull → extract → imports → reports). |

> Note: **Shortcut + I** is used in two menus (**Edit** and **Process**). JavaFX resolves accelerators globally per scene, so whichever menu item is registered last may win. If both are present, one may override the other depending on load order.

---

### Help Menu

| Shortcut | Action | Notes |
|---------|--------|------|
| **Shortcut + H** | About | Opens the help/manual window (README). |

---

## Inline Editing Shortcuts

These apply when editing list entries.

### Assignments List

| Shortcut | Action |
|---------|--------|
| Double-click on an assignment | Start editing assignment name (inline). |
| Enter (while editing) | Commit edit (save changes). |
| Click outside editor / lose focus | Cancel edit (discard changes). |

### Files List

| Shortcut | Action |
|---------|--------|
| Double-click on a file | Start editing file name (inline). |
| Enter (while editing) | Commit edit (save changes). |
| Click outside editor / lose focus | Cancel edit (discard changes). |

---

## Platform Notes

- **Shortcut** means:
  - **Command (⌘)** on macOS
  - **Control (Ctrl)** on Windows/Linux
- These shortcuts work anywhere in the main window because they are registered as **menu accelerators**.

