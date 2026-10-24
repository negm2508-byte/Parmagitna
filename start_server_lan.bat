@echo off
echo ==============================================================================
echo PaperERP Enterprise -- Local LAN Server Launcher
echo Host IP: 192.168.9.223 (Access from any PC/Tablet on the factory network)
echo Frontend URL: http://192.168.9.223:5173
echo Backend API : http://192.168.9.223:8080/api/v1/health
echo ==============================================================================
echo.

echo [1/2] Starting Spring Boot Backend on 0.0.0.0:8080...
start "PaperERP Backend (8080)" cmd /k "cd /d %~dp0backend && mvnw.cmd spring-boot:run"

echo [2/2] Starting Vite Frontend on 0.0.0.0:5173...
start "PaperERP Frontend (5173)" cmd /k "cd /d %~dp0frontend && npm run dev -- --host 0.0.0.0"

echo.
echo ==============================================================================
echo Servers launched successfully in separate background console windows!
echo - Local Browser: http://localhost:5173
echo - Factory LAN  : http://192.168.9.223:5173
echo ==============================================================================
