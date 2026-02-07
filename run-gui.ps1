$ErrorActionPreference = "Stop"

Write-Host "Building core..."
mvn -q -pl core -am -DskipTests package

Write-Host "Building classpath..."
Push-Location core
mvn -q -DincludeScope=runtime "-Dmdep.outputFile=cp.txt" dependency:build-classpath
$cp = "target/classes;" + (Get-Content cp.txt)
Pop-Location

Write-Host "Running GUI..."
& 'C:\Program Files\Eclipse Adoptium\jdk-11.0.29.7-hotspot\bin\java.exe' -cp $cp gui.DmTestGui
