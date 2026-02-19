# Deploy Firebase Cloud Functions for ToyyibPay Integration

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "   ToyyibPay Cloud Functions Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Navigate to functions directory
Write-Host "📁 Navigating to functions directory..." -ForegroundColor Yellow
Set-Location -Path "$PSScriptRoot\functions"

# Check if firebase CLI is installed
Write-Host "🔍 Checking Firebase CLI..." -ForegroundColor Yellow
try {
    $firebaseVersion = firebase --version
    Write-Host "✅ Firebase CLI found: $firebaseVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Firebase CLI not found. Please install it first:" -ForegroundColor Red
    Write-Host "   npm install -g firebase-tools" -ForegroundColor Yellow
    exit 1
}

# Install dependencies
Write-Host ""
Write-Host "📦 Installing dependencies..." -ForegroundColor Yellow
npm install

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to install dependencies" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Dependencies installed" -ForegroundColor Green

# Build TypeScript
Write-Host ""
Write-Host "🔨 Building TypeScript..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful" -ForegroundColor Green

# Deploy functions
Write-Host ""
Write-Host "🚀 Deploying Cloud Functions..." -ForegroundColor Yellow
Write-Host "   Functions: createToyyibPayBill, paymentCallback" -ForegroundColor Cyan

firebase deploy --only functions:createToyyibPayBill,functions:paymentCallback

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Deployment failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "   ✅ DEPLOYMENT SUCCESSFUL" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Your Cloud Functions are now live at:" -ForegroundColor Cyan
Write-Host "https://us-central1-wingzone-app.cloudfunctions.net" -ForegroundColor White
Write-Host ""
Write-Host "Functions deployed:" -ForegroundColor Cyan
Write-Host "  • createToyyibPayBill - Creates payment bills" -ForegroundColor Whitez    
Write-Host "  • paymentCallback - Receives payment confirmations" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. Test payment flow in the Android app" -ForegroundColor White
Write-Host "  2. Configure ToyyibPay webhook URL in dashboard" -ForegroundColor White
Write-Host "  3. Monitor logs: firebase functions:log" -ForegroundColor White
Write-Host ""

# Return to root directory
Set-Location -Path $PSScriptRoot
