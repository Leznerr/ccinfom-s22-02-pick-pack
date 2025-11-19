@echo off
setlocal enabledelayedexpansion

REM === Repo root (handles spaces/apostrophes) ===
set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"
set "APP_DIR=%ROOT%\CCINFOM S22-02-DBAPP"

REM === Config (password will be prompted) ===
set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_SCHEMA=ccinfom_dev"
set "DB_USER=root"
set "MYSQL_EXE=C:\Program Files\MySQL\bin\mysql.exe"  REM override to full path if not on PATH

set /p "DB_PASSWORD=Enter DB password: "
set "JAVA_OPTS=-Ddb.host=%DB_HOST% -Ddb.port=%DB_PORT% -Ddb.schema=%DB_SCHEMA% -Ddb.user=%DB_USER% -Ddb.password=%DB_PASSWORD%"

REM === Locate javac ===
set "JAVAC_BIN="
if defined JAVA_HOME set "JAVA_HOME=%JAVA_HOME:"=%"
if defined JAVAC_BIN set "JAVAC_BIN=%JAVAC_BIN:"=%"
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_BIN=%JAVA_HOME%\bin\javac.exe"
)
if not defined JAVAC_BIN (
  for /f "delims=" %%d in ('dir /b /ad "C:\Program Files\Java\jdk*" 2^>nul') do (
    if not defined JAVAC_BIN if exist "C:\Program Files\Java\%%d\bin\javac.exe" set "JAVAC_BIN=C:\Program Files\Java\%%d\bin\javac.exe"
  )
)
if not defined JAVAC_BIN for /f "usebackq tokens=1*" %%i in (`where javac.exe 2^>nul`) do if not defined JAVAC_BIN set "JAVAC_BIN=%%i"
if not defined JAVAC_BIN (
  echo [!] javac not found. Install JDK 17+ and/or set JAVA_HOME.
  goto :eof
)

REM === Optional: reset + seed (uncomment to use) ===
REM call :run_sql "%ROOT%\db\migrations\reset.sql" "" || goto :eof
REM call :run_sql "%ROOT%\db\seed\cores.sql" "%DB_SCHEMA%" || goto :eof

REM === Compile ===
echo [*] Building Java sources...
powershell -NoProfile -Command ^
  "$root = [IO.Path]::GetFullPath(\"%ROOT%\");" ^
  "$src  = Join-Path $root 'CCINFOM S22-02-DBAPP\src\main\java';" ^
  "$out  = Join-Path $root 'sources.txt';" ^
  "Get-ChildItem -LiteralPath $src -Recurse -File -Filter *.java | ForEach-Object { '\"{0}\"' -f ($_.FullName -replace '\\','/') } | Set-Content -Encoding ASCII $out"
if errorlevel 1 goto :eof

"%JAVAC_BIN%" -cp "%APP_DIR%\lib/*" -d "%APP_DIR%\out" @"%ROOT%\sources.txt"
if errorlevel 1 goto :eof

REM === Run ===
echo [*] Launching app...
set "CLASSPATH=%APP_DIR%\out;%APP_DIR%\lib/*;%APP_DIR%\src/main/resources"
echo [*] Using DB host=%DB_HOST%, user=%DB_USER%
java %JAVA_OPTS% -cp "%CLASSPATH%" com.ccinfom.ui.MainApp
goto :eof

:run_sql
REM %1 = sql file, %2 = schema (optional)
if not exist "%~1" (
  echo [!] SQL file not found: %~1
  exit /b 1
)
if "%~2"=="" (
  type "%~1" | "%MYSQL_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p
) else (
  type "%~1" | "%MYSQL_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p "%~2"
)
exit /b %errorlevel%

endlocal
