@echo off
setlocal

set SCRIPT_DIR=%~dp0
set PROTOBUF_TAG=v34.0
set WORK_DIR=%TEMP%\pb-%RANDOM%
set PROTOBUF_DIR=%WORK_DIR%\src
set BUILD_DIR=%WORK_DIR%\build

:: Clone fresh
echo Cloning protobuf %PROTOBUF_TAG% to %PROTOBUF_DIR%...
git clone --depth 1 --branch %PROTOBUF_TAG% https://github.com/protocolbuffers/protobuf.git "%PROTOBUF_DIR%"
if errorlevel 1 goto :error

:: Build in temp (short path avoids MSVC 250 char limit)
mkdir "%BUILD_DIR%"
cd /d "%BUILD_DIR%"

echo Configuring CMake...
cmake "%SCRIPT_DIR%." -G Ninja -DCMAKE_BUILD_TYPE=Release -DPROTOBUF_SRC_DIR="%PROTOBUF_DIR%"
if errorlevel 1 goto :cleanup

echo Building protoc_bridge...
cmake --build . --config Release
if errorlevel 1 goto :cleanup

:: Copy DLL to test resources
set OUTPUT_DIR=%SCRIPT_DIR%..\src\test\resources\native
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"
copy /y protoc_bridge.dll "%OUTPUT_DIR%\protoc_bridge.dll" >nul
echo.
echo Build complete: %OUTPUT_DIR%\protoc_bridge.dll
goto :cleanup

:error
echo.
echo BUILD FAILED

:cleanup
cd /d "%SCRIPT_DIR%"
echo Cleaning up %WORK_DIR%...
rmdir /s /q "%WORK_DIR%" 2>nul
