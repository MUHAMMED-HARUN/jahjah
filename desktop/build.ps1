# Builds the layers in order. Each layer only sees the layers it may depend on:
#   SharedEntity  <-  DAL  <-  BAL  <-  UI   (UI cannot see DAL)
$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$out  = Join-Path $here 'out'

function Build-Layer($name, $classpath) {
    $src  = Join-Path $here "$name\src"
    $dest = Join-Path $out $name
    if (Test-Path $dest) { Remove-Item -Recurse -Force $dest }
    New-Item -ItemType Directory -Force $dest | Out-Null
    $files = Get-ChildItem -Recurse -Filter *.java $src | ForEach-Object { $_.FullName }
    $javacArgs = @('-encoding', 'UTF-8', '--release', '21', '-d', $dest)
    if ($classpath) { $javacArgs += @('-cp', ($classpath -join ';')) }
    & javac @javacArgs @files
    if ($LASTEXITCODE -ne 0) { throw "Build failed: $name" }
    Write-Host "OK  $name"
}

Build-Layer 'SharedEntity' $null
Build-Layer 'DAL' @("$out\SharedEntity")
Build-Layer 'BAL' @("$out\SharedEntity", "$out\DAL")
Build-Layer 'UI'  @("$out\SharedEntity", "$out\BAL")

# One runnable jar containing all four layers
$all = Join-Path $out 'all'
if (Test-Path $all) { Remove-Item -Recurse -Force $all }
New-Item -ItemType Directory -Force $all | Out-Null
foreach ($l in 'SharedEntity', 'DAL', 'BAL', 'UI') { Copy-Item -Recurse -Force "$out\$l\*" $all }

$jarPath = Join-Path $out 'products-manager.jar'
if (Test-Path $jarPath) {
    try { [IO.File]::Open($jarPath, 'Open', 'ReadWrite', 'None').Close() }
    catch { throw 'The app is still open (products-manager.jar is in use). Close it and build again.' }
}
& jar --create --file "$out\products-manager.jar" --main-class jahjah.ui.Main -C $all .
if ($LASTEXITCODE -ne 0) { throw 'jar creation failed' }
Write-Host "OK  $out\products-manager.jar"
