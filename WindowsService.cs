using System;
using System.Data.SQLite;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.ServiceProcess;

namespace ShutdownTimeUpdaterService
{
    public partial class ShutdownTimeService : ServiceBase
    {
        private string _cachedUsername;

        public ShutdownTimeService()
        {
            InitializeComponent();
        }

        protected override void OnStart(string[] args)
        {
            _cachedUsername = GetLoggedInUsername();

            if (string.IsNullOrEmpty(_cachedUsername))
            {
                EventLog.WriteEntry("ShutdownTimeService", "Could not detect logged-in user at service start.", EventLogEntryType.Warning);
            }
            else
            {
                EventLog.WriteEntry("ShutdownTimeService", $"Detected user at start: {_cachedUsername}", EventLogEntryType.Information);
            }

            EventLog.WriteEntry("ShutdownTimeService", "Service started.", EventLogEntryType.Information);
        }

        protected override void OnStop()
        {
            EventLog.WriteEntry("ShutdownTimeService", "Service stopped.", EventLogEntryType.Information);
        }

        protected override void OnShutdown()
        {
            EventLog.WriteEntry("ShutdownTimeService", "System is shutting down. Attempting to update logout time.", EventLogEntryType.Information);
            UpdateLogoutTime();
        }

        private void UpdateLogoutTime()
        {
            try
            {
                string dbPath = GetUserAppDataPath();

                if (string.IsNullOrEmpty(dbPath) || !File.Exists(dbPath))
                {
                    EventLog.WriteEntry("ShutdownTimeService", $"Database file not found or invalid path: {dbPath}", EventLogEntryType.Error);
                    return;
                }

                using (var conn = new SQLiteConnection($"Data Source={dbPath};Version=3;"))
                {
                    conn.Open();

                    string updateQuery = "UPDATE sessions SET logout_time = @LogoutTime WHERE logout_time IS NULL LIMIT 1";

                    using (var cmd = new SQLiteCommand(updateQuery, conn))
                    {
                        cmd.Parameters.AddWithValue("@LogoutTime", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss"));

                        int rowsAffected = cmd.ExecuteNonQuery();

                        if (rowsAffected > 0)
                        {
                            EventLog.WriteEntry("ShutdownTimeService", "Logout time updated for active session.", EventLogEntryType.Information);
                        }
                        else
                        {
                            EventLog.WriteEntry("ShutdownTimeService", "No active session found to update logout time.", EventLogEntryType.Warning);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                EventLog.WriteEntry("ShutdownTimeService", $"Error updating logout time: {ex.Message}\nStackTrace: {ex.StackTrace}", EventLogEntryType.Error);
            }
        }

        private string GetUserAppDataPath()
        {
            if (string.IsNullOrEmpty(_cachedUsername))
            {
                EventLog.WriteEntry("ShutdownTimeService", "Cached username is null or empty.", EventLogEntryType.Error);
                return null;
            }

            string userAppDataPath = Path.Combine(@"C:\Users", _cachedUsername, "AppData", "Roaming", "SessionTracker", "user_sessions.db");
            return userAppDataPath;
        }

        // Get the username of the currently logged-in user
        [DllImport("Wtsapi32.dll")]
        static extern bool WTSQuerySessionInformation(IntPtr hServer, int sessionId, WTS_INFO_CLASS wtsInfoClass, out IntPtr ppBuffer, out int pBytesReturned);

        [DllImport("kernel32.dll")]
        static extern int WTSGetActiveConsoleSessionId();

        [DllImport("Wtsapi32.dll")]
        static extern void WTSFreeMemory(IntPtr pointer);

        enum WTS_INFO_CLASS
        {
            WTSUserName = 5,
            WTSDomainName = 7,
        }

        private string GetLoggedInUsername()
        {
            int sessionId = WTSGetActiveConsoleSessionId();
            IntPtr buffer;
            int strLen;

            if (WTSQuerySessionInformation(IntPtr.Zero, sessionId, WTS_INFO_CLASS.WTSUserName, out buffer, out strLen))
            {
                string userName = Marshal.PtrToStringAnsi(buffer);
                WTSFreeMemory(buffer);
                return userName;
            }

            return null;
        }
    }
}

