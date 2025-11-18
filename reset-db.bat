@echo off
setlocal

set "ROOT=%~dp0"
if "%ROOT:~-1%"=="\" set "ROOT=%ROOT:~0,-1%"

set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_SCHEMA=ccinfom_dev"
set "DB_USER=root"
set "MYSQL_EXE=C:\Program Files\MySQL\bin\mysql.exe"  REM adjust if needed

set /p "DB_PASSWORD=Enter DB password: "

echo [*] Resetting schema "%DB_SCHEMA%" on %DB_HOST%:%DB_PORT% ...
type "%ROOT%\db\migrations\reset.sql" | "%MYSQL_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD%
if errorlevel 1 goto :eof

echo [*] Seeding cores into "%DB_SCHEMA%" ...
type "%ROOT%\db\seed\cores.sql" | "%MYSQL_EXE%" -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASSWORD% %DB_SCHEMA%
if errorlevel 1 goto :eof

echo [*] Done.
endlocal
