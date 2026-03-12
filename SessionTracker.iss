[Setup]
AppName=SessionTracker
AppVersion=1.0
DefaultDirName={pf}\SessionTracker
OutputDir=.
OutputBaseFilename=SessionTrackerInstaller
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64

[Files]
Source: "gui\app-gui.jar"; DestDir: "{app}\gui"; Flags: ignoreversion
Source: "gui\config.properties"; DestDir: "{app}\gui"; Flags: ignoreversion
Source: "jre\*"; DestDir: "{app}\jre"; Flags: recursesubdirs ignoreversion
Source: "service\*"; DestDir: "{app}\service"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "install.bat"; DestDir: "{app}"; Flags: ignoreversion
Source: "uninstall.bat"; DestDir: "{app}"; Flags: ignoreversion

[Run]
Filename: "cmd.exe"; Parameters: "/c install.bat"; WorkingDir: "{app}"; Flags: runhidden waituntilterminated

[UninstallRun]
Filename: "cmd.exe"; Parameters: "/c uninstall.bat"; WorkingDir: "{app}"; Flags: runhidden waituntilterminated runascurrentuser

[Code]

var
  SystemNoPage: TInputQueryWizardPage;

procedure InitializeWizard();
begin
  SystemNoPage := CreateInputQueryPage(
    wpSelectDir,
    'System Configuration',
    'Configure System Number',
    'Please enter the SYSTEM_NO value for this installation.'
  );

  SystemNoPage.Add('SYSTEM_NO:', False);

  { Default shown in installer }
  SystemNoPage.Values[0] := 'LAB-M1';
end;


procedure ReplaceSystemNoInFile(FileName, NewValue: string);
var
  Lines: TArrayOfString;
  I: Integer;
begin
  if LoadStringsFromFile(FileName, Lines) then
  begin
    for I := 0 to GetArrayLength(Lines) - 1 do
    begin
      if Pos('SYSTEM_NO=', Lines[I]) = 1 then
        Lines[I] := 'SYSTEM_NO=' + NewValue;
    end;

    SaveStringsToFile(FileName, Lines, False);
  end
  else
  begin
    MsgBox('Could not open config.properties file.', mbError, MB_OK);
  end;
end;


procedure CurStepChanged(CurStep: TSetupStep);
var
  ConfigPath: string;
  SystemNo: string;
begin
  if CurStep = ssPostInstall then
  begin
    SystemNo := SystemNoPage.Values[0];
    ConfigPath := ExpandConstant('{app}\gui\config.properties');

    ReplaceSystemNoInFile(ConfigPath, SystemNo);
  end;
end;
