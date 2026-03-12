@echo off
echo Removing service...
sc stop SessionTrackerService
sc delete SessionTrackerService

echo Removing GUI app from startup...
reg delete "HKCU\Software\Microsoft\Windows\CurrentVersion\Run" /v SessionTracker /f