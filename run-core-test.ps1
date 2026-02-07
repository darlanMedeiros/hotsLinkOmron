$ErrorActionPreference = "Stop"

Write-Host "Building core..."
mvn -q -pl core -am -DskipTests package

Write-Host "Building classpath (core)..."
Push-Location core
mvn -q -DincludeScope=runtime "-Dmdep.outputFile=cp.txt" dependency:build-classpath
$cp = "target/classes;" + (Get-Content cp.txt)
Pop-Location

if (-not $args -or $args.Count -eq 0) {
  Write-Host "Usage: .\\run-core-test.ps1 <MainClass> [args...]"
  Write-Host "Example: .\\run-core-test.ps1 demo.TestDmDbWrite COM2 9600 7 2 EVEN 0 10000 1 50"
  exit 1
}

$mainClass = $args[0]
$mainArgs = @()
if ($args.Count -gt 1) { $mainArgs = $args[1..($args.Count-1)] }

Write-Host "Running $mainClass..."
& 'C:\\Program Files\\Eclipse Adoptium\\jdk-11.0.29.7-hotspot\\bin\\java.exe' -cp $cp $mainClass @mainArgs
