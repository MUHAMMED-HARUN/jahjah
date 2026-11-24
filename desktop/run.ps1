# Runs the app on this project's data (parent folder: "data base" and "images")
$ErrorActionPreference = 'Stop'
$jar = Join-Path $PSScriptRoot 'out\products-manager.jar'
if (-not (Test-Path $jar)) { & (Join-Path $PSScriptRoot 'build.ps1') }
$root = Split-Path $PSScriptRoot -Parent
& java -Dfile.encoding=UTF-8 -jar $jar $root
