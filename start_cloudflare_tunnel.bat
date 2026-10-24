@echo off
echo ==============================================================================
echo PaperERP Enterprise -- Cloudflare Live Demo Tunnel
echo Connecting local ERP server to Cloudflare for remote client access...
echo ==============================================================================
echo.

IF NOT EXIST "C:\Users\IT3_Helpdesk\.tools\cloudflared.exe" (
    echo Cloudflare tunnel executable not found. Running setup script...
    powershell -ExecutionPolicy Bypass -File "%~dp0\.tools\setup_cloudflared.ps1"
)

echo Starting tunnel on http://localhost:5173...
echo Copy the generated https://*.trycloudflare.com link to share with your client!
echo.

"C:\Users\IT3_Helpdesk\.tools\cloudflared.exe" tunnel --url http://localhost:5173
