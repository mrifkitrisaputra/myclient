@echo off
cd /d "%~dp0"

echo ==========================================
echo      KONFIGURASI MANUAL
echo ==========================================

:: =========================================================================
:: [PENTING] GANTI PATH DI BAWAH INI DENGAN LOKASI JAVAFX KAMU
:: Contoh: set JAVAFX_LIB=C:\Program Files\Java\javafx-sdk-21.0.1\lib
:: =========================================================================
set JAVAFX_LIB="C:\Users\User\Downloads\openjfx-21.0.9_windows-x64_bin-sdk\javafx-sdk-21.0.9\lib"

:: Cek apakah path benar
if not exist "%JAVAFX_LIB%" (
    echo [ERROR] Folder JavaFX tidak ditemukan di:
    echo %JAVAFX_LIB%
    echo.
    echo Tolong edit file build.bat ini dan perbaiki baris 'set JAVAFX_LIB=...'
    pause
    exit /b
)

echo JavaFX Path: %JAVAFX_LIB%
echo.

echo ==========================================
echo      STEP 1: BERSIH-BERSIH (CLEAN)
echo ==========================================
if exist bin rmdir /s /q bin
if exist Client.jar del Client.jar
if exist Server.jar del Server.jar
if exist manifest_client.txt del manifest_client.txt
if exist manifest_server.txt del manifest_server.txt
if exist sources.txt del sources.txt
mkdir bin

echo.
echo ==========================================
echo      STEP 2: CARI FILE JAVA
echo ==========================================
:: Cari semua file .java di folder src secara otomatis
dir /s /B "src\main\java\*.java" > sources.txt

echo.
echo ==========================================
echo      STEP 3: COMPILING...
echo ==========================================
javac -d bin --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.media @sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Gagal Compile! Cek error di atas.
    del sources.txt
    pause
    exit /b
)

del sources.txt
echo Compile sukses.

echo.
echo ==========================================
echo      STEP 4: COPY RESOURCES
echo ==========================================
xcopy "src\main\resources\*" "bin\" /s /e /y /q

echo.
echo ==========================================
echo      STEP 5: BIKIN MANIFEST
echo ==========================================
echo Main-Class: com.client.App> manifest_client.txt
echo.>> manifest_client.txt

echo Main-Class: com.client.server.SimpleTestServer> manifest_server.txt
echo.>> manifest_server.txt

echo.
echo ==========================================
echo      STEP 6: BIKIN JAR (PACKAGING)
echo ==========================================

:: Bikin Client.jar (Semua file)
jar cvfm Client.jar manifest_client.txt -C bin .

echo.
echo ------------------------------------------

:: Bikin Server.jar (Khusus folder server)
jar cvfm Server.jar manifest_server.txt -C bin com/client/server

echo.
echo ==========================================
echo      SELESAI!
echo ==========================================
echo.
echo Cara menjalankan Client:
echo java --module-path "%JAVAFX_LIB%" --add-modules javafx.controls,javafx.fxml,javafx.media -jar Client.jar
echo.
echo Cara menjalankan Server:
echo java -jar Server.jar
echo.
pause