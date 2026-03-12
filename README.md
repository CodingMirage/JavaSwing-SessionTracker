# 📚 Session Tracker (DigiLogBook Client)

<div align="center">

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge\&logo=openjdk\&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-GUI-blue?style=for-the-badge)
![SQLite](https://img.shields.io/badge/SQLite-07405E?style=for-the-badge\&logo=sqlite\&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge\&logo=mysql\&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

</div>

**A modern, offline-first desktop application and background Windows service for automating lab session logbooks with seamless Supabase cloud synchronization.**

---

# 📑 Table of Contents

1. [About the Project](#-about-the-project)
2. [Distributed System Architecture](#-distributed-system-architecture)
3. [Key Features](#-key-features)
4. [Technology Stack](#%EF%B8%8F-technology-stack)
5. [Detailed System Workflow](#-detailed-system-workflow)
6. [Configuration & Deployment](#%EF%B8%8F-configuration--deployment)
7. [Core Mechanics & Data Flow](#%EF%B8%8F-core-mechanics--data-flow)
8. [Project Structure & Key Files](#%EF%B8%8F-project-structure--key-files)
9. [Installation & Deployment (Inno Setup)](#-installation--deployment-inno-setup)
10. [First-Time Usage & Configuration](#-first-time-usage--configuration)
11. [Future Enhancements & Roadmap](#-future-enhancements--roadmap)
12. [License & Acknowledgments](#-license--acknowledgments)

---

# 📖 About the Project

Session Tracker is an automated, Java-based system designed to eliminate manual logbooks in college computer labs and shared institutional environments. By combining a lightweight graphical interface with a persistent background Windows service, the application accurately records student lab usage—capturing precise login and logout times—without disrupting the user's workflow.

The system is built with a resilient **offline-first architecture**, utilizing an embedded SQLite database to guarantee data integrity during network outages or sudden power failures. It automatically synchronizes with a centralized Supabase PostgreSQL cloud database whenever a secure connection is established.

---

# 🔗 Distributed System Architecture

This project is the client-side module of the broader **DigiLogBook** ecosystem. The ecosystem is divided into two primary repositories to ensure a clean separation of concerns:

* **Client - Session Tracker (This Repo):** Developed by [@CodingMirage](https://github.com/CodingMirage). Handles the desktop user interface, local data persistence, Windows system event tracking (startup/shutdown), and local-to-cloud synchronization triggers.

* **Server - DigiLogBook:** Developed by [@mohammedrayyan12](https://github.com/mohammedrayyan12). Manages the Supabase cloud infrastructure, overarching data persistence, fail-safe synchronization logic, and API endpoints.
  *(View the [Server Repository](https://github.com/mohammedrayyan12/JavaSwing-DigiLogBook))*

---

# ✨ Key Features

🚀 **Zero-Friction Auto-Start**
Automatically launches on Windows boot via bundled batch script registry modifications.

🖥️ **Lightweight & Unobtrusive UI**
A fast Java Swing interface that prompts for credentials and immediately terminates to free up system memory and CPU.

🕒 **Automated Lifecycle Tracking**
Accurately captures both user login (via the UI prompt) and system shutdown (via a background Windows Service).

🗄️ **Robust Local Storage**
Built-in SQLite database ensures zero data loss during power failures or internet drops.

☁️ **Intelligent Cloud Sync**
Leverages REST APIs and Supabase Edge Functions to push data to a PostgreSQL cloud database at strategic lifecycle events (boot and login).

📦 **Plug-and-Play Deployment**
Packaged as a single standalone `.exe` containing the application JAR, JRE, SQLite, Windows Service, and registry scripts.

⚙️ **Highly Configurable**
Organization-specific variables (database links, Supabase Anon keys) are isolated in an external configuration file, allowing easy scaling across different institutions without recompiling the Java code.

---

# 🛠️ Technology Stack

### Application Core

* **Java (Swing)** — User interface and core application execution logic
* **Gradle** — Build automation tool used to compile the project, resolve dependencies, and generate the executable Fat JAR
* **GSON** — For JSON parsing, serialization, and structuring API payloads

### System Integration

* **Windows Service** — Background daemon process for detecting OS shutdown/logoff events
* **Batch Scripting (.bat)** — For automated system registration, service installation, and environment setup

### Data & Synchronization

* **SQLite** — Local, embedded relational database for offline-first persistence
* **PostgreSQL** — Primary database engine used within the cloud environment
* **Supabase** — Cloud backend infrastructure providing the PostgreSQL database and API routing
* **Supabase Edge Functions** — Serverless functions facilitating secure data synchronization and validation

---

# 🧠 Detailed System Workflow

The Session Tracker operates on a highly specific event-driven lifecycle to ensure data accuracy and minimal user interruption:

### 1. System Boot & Initialization

When the Windows PC starts, the pre-registered Java Swing application launches automatically.

### 2. Initial Cloud Sync

Before displaying the UI, the app performs a preliminary sync with the Supabase cloud, pushing any incomplete session data left over from previous offline usage.

### 3. Data Entry Prompt

The application presents a clean GUI prompting the student for their **Name** and **USN** (University Serial Number).

### 4. Local Commit & Termination

Upon form submission, the exact login timestamp and user details are securely saved to the local SQLite database. A secondary cloud sync is triggered immediately. The GUI application then terminates entirely so the student can work without background UI processes consuming resources.

### 5. Background Monitoring (The Windows Service)

A standalone Windows Service runs silently in the background, continuously monitoring the system's power state.

### 6. System Shutdown & Logout Capture

When the user powers off or logs out of the PC, the Windows Service intercepts the OS termination event. It logs the exact termination time and updates the local SQLite record for that specific active session.

### 7. Reconciliation (Next Boot)

On the subsequent system boot, the updated session record (now containing the complete login/logout lifecycle) is pushed to the cloud DB during Step 2.

---

# ⚙️ Configuration & Deployment

This application is designed for frictionless deployment by IT administrators across multiple lab environments. The entire application is packaged into a self-contained executable.

### The Deployment Package Includes

* Compiled Java Application (Fat JAR containing all dependencies)
* Bundled Java Runtime Environment (JRE)
* Windows Service executable
* Automated `.bat` scripts
* External `config` file

---

# ⚙️ Core Mechanics & Data Flow

## 1. The Boot Sequence (`Main.java`)

1. `setupDatabase()` — Initializes SQLite and ensures the `sessions` table exists
2. `syncOnce()` — Downloads configuration from Supabase edge function
3. `syncLocalDataToRemote()` — Pushes unsynced local records to the cloud
4. **GUI Launch** — Starts the Swing UI (`MyFrame`)

---

## 2. Smart USN Decoding

When a student inputs their USN (example: `1VI21CS045`), the application validates it using:

```
^1VI\d{2}[A-Z]{2}\d{3}$
```

The system automatically derives:

* Semester
* Department
* Batch

based on the current year and USN structure.

---

## 3. State Management

To prevent excessive cloud requests, the system uses:

```
config_synced.flag
```

stored in:

```
%APPDATA%\SessionTracker
```

Configuration is downloaded **only once per machine** unless manually reset via the **Sync button**.

---

# 🗂️ Project Structure & Key Files

```
SessionTracker/
├── src/main/java/
│   ├── Main.java
│   ├── AppBackend.java
│   ├── CloudDatabaseUpload.java
│   ├── ConfigLoader.java
│   ├── ConfigSyncManager.java
│   ├── MyFrame.java / MyPanel.java
│
├── gui/
│   ├── app-gui.jar
│   ├── config.properties
│
├── inno script.txt
└── install.bat / uninstall.bat
```

---

# 📦 Installation & Deployment (Inno Setup)

Deploying to dozens of lab machines manually is tedious. This project uses **Inno Setup** to compile a **single installer executable**.

### Directory Preparation

Ensure the following structure exists before compiling:

```
gui/app-gui.jar
gui/config.properties
jre/
service/
install.bat
uninstall.bat
```

### Installation Process

1. Run **SessionTrackerInstaller.exe**
2. Enter **SYSTEM_NO** (example: `LAB-M1`)
3. Installer automatically updates `config.properties`
4. `install.bat` runs silently to register the service

---

# 🚀 First-Time Usage & Configuration

## config.properties

Example configuration:

```
LOCAL_TABLE=sessions
LOCAL_TABLE_CONFIG=CONFIGURATION_TABLE
LOCAL_DB=user_sessions.db
CLOUD_FUNCTION_INSERT=insert-records
CLOUD_FUNCTION_SYNC=fetch-all-config
PROJECT_URL=https://[YOUR_SUPABASE_ID].supabase.co
ANON_KEY=[YOUR_JWT_ANON_KEY]
ADMIN_PASSWORD=root
SYSTEM_NO=M-1
LAB_NAME=Lab
```

---

## First Run

On first launch:

* `ConfigSyncManager.syncOnce()` downloads cloud configuration
* Creates `config_synced.flag`
* Stored in:

```
%APPDATA%\SessionTracker
```

To refresh configuration:

* Click **Sync** in UI
* Or login to **Admin panel**

---

# 🔮 Future Enhancements & Roadmap

Potential improvements:

### Admin Dashboard UI

Allow administrators to view local SQLite logs directly from the GUI.

### Auto-Updater

Check GitHub Releases for new JAR versions.

### Encrypted Local Storage

Integrate **SQLCipher** to encrypt `user_sessions.db`.

### Real-Time Tracking

Upgrade REST sync to **Supabase Realtime WebSocket** to display live lab occupancy dashboards.

---

# 📄 License & Acknowledgments

Distributed under the **MIT License**.

**Acknowledgments**

* Icons provided by **Shields.io**
* Database infrastructure powered by **Supabase**

---

✅ This version will render **perfectly on GitHub** with:

* Working **Table of Contents links**
* Proper **section hierarchy**
* Clean **developer documentation structure**

---
