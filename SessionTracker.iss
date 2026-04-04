[Setup]
AppName=SessionTracker
AppVersion=1.0
DefaultDirName={autopf}\SessionTracker
OutputDir=.
OutputBaseFilename=SessionTrackerInstaller
Compression=lzma
SolidCompression=yes
PrivilegesRequired=admin
ArchitecturesInstallIn64BitMode=x64compatible

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
Filename: "cmd.exe"; Parameters: "/c uninstall.bat"; WorkingDir: "{app}"; Flags: runhidden waituntilterminated runascurrentuser; RunOnceId: "UninstallService"

[Code]

var
  SystemConfigPage: TInputQueryWizardPage;

procedure InitializeWizard();
begin
  SystemConfigPage := CreateInputQueryPage(
    wpSelectDir,
    'System Configuration',
    'Configure System Information',
    'Please enter the System No and Lab Name for this installation.'
  );

  { System No field }
  SystemConfigPage.Add('System No:', False);

  { Lab Name field }
  SystemConfigPage.Add('Lab Name:', False);
end;


function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = SystemConfigPage.ID then
  begin
    if Trim(SystemConfigPage.Values[0]) = '' then
    begin
      MsgBox('Please enter the System No.', mbError, MB_OK);
      Result := False;
      Exit;
    end;

    if Trim(SystemConfigPage.Values[1]) = '' then
    begin
      MsgBox('Please enter the Lab Name.', mbError, MB_OK);
      Result := False;
      Exit;
    end;
  end;
end;


procedure ReplaceConfigValue(FileName, Key, NewValue: string);
var
  Lines: TArrayOfString;
  I: Integer;
begin
  if LoadStringsFromFile(FileName, Lines) then
  begin
    for I := 0 to GetArrayLength(Lines) - 1 do
    begin
      if Pos(Key + '=', Lines[I]) = 1 then
        Lines[I] := Key + '=' + NewValue;
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
  LabName: string;
begin
  if CurStep = ssPostInstall then
  begin
    SystemNo := SystemConfigPage.Values[0];
    LabName := SystemConfigPage.Values[1];

    ConfigPath := ExpandConstant('{app}\gui\config.properties');

    ReplaceConfigValue(ConfigPath, 'SYSTEM_NO', SystemNo);
    ReplaceConfigValue(ConfigPath, 'LAB_NAME', LabName);
  end;
end;