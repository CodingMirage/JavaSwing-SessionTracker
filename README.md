### Session Tracker – Automated Lab Usage Logging System

Session Tracker is a Java-based desktop application designed to **automatically record student lab usage sessions** on Windows systems, eliminating the need for manual logbooks in college computer labs.

The application consists of two main components:

* A **Java Swing startup application** that launches automatically when Windows boots
* A **background Windows service** that tracks system shutdown events

When the system starts, the Swing application prompts the student to enter their **Name** and **USN**. Once submitted, the application records the **login time** and closes immediately, allowing normal system usage without interruption.

A background Windows service continues running silently and captures the **shutdown time** when the system is powered off. Together, these timestamps form a complete session record.

All session data is:

* Stored locally using **SQLite**
* Automatically synced to the **cloud** whenever an internet connection is available

This ensures reliable data collection even in offline environments.

---

## ✨ Key Features

* 🚀 **Auto-start on Windows boot**
* 🖥️ **Lightweight Java Swing UI**
* 🕒 **Automatic login & shutdown time tracking**
* 🗄️ **Local storage using SQLite**
* ☁️ **Offline-first with automatic cloud sync**
* 🔒 **No manual intervention required after login**
* 📚 **Designed for college computer labs**

---

## 🛠️ Tech Stack

* **Java (Swing)**
* **Windows Service**
* **SQLite (Local Database)**
* **Cloud Storage / API** (for session sync)
* **Windows OS**

---

## 🎯 Objective

The primary goal of Session Tracker is to **replace manual lab logbooks** with a fully automated, accurate, and reliable digital system that:

* Reduces human error
* Saves time for students and lab staff
* Provides structured session data for analysis and reporting

---

## 📌 Use Case

Ideal for:

* College and university computer labs
* Training centers
* Shared institutional systems requiring usage tracking

---

## ⚠️ Note

This project was developed as a **college academic project** and may require administrator privileges for installing the Windows service and configuring auto-start behavior.

---
