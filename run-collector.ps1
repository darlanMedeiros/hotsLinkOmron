$ErrorActionPreference = "Stop"

Write-Host "Compilando o projeto usando Maven..."
mvn clean compile -DskipTests

Write-Host "Iniciando o Collector Multi PLC..."
mvn exec:java -pl collector "-Dexec.mainClass=org.omron.collector.CollectorMultPlcAplication"
