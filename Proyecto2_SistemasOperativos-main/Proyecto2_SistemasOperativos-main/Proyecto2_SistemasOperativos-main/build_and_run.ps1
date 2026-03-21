cd c:\Users\tomas\Downloads\Proyecto2_SO-main\Proyecto2_SistemasOperativos-main\Proyecto2_SistemasOperativos-main\Proyecto2_SistemasOperativos-main\src\main\java
$files = Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
[System.IO.File]::WriteAllLines("$(Get-Location)\sources.txt", $files)
cmd /c 'javac -encoding UTF-8 -cp "..\..\..\json-20231013.jar" @sources.txt'
if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilación Exitosa. Ejecutando la aplicación..."
    Start-Process java -ArgumentList "-cp `"..\..\..\json-20231013.jar;.`" com.mycompany.so_proyecto2.SO_Proyecto2"
} else {
    Write-Host "Error en compilación."
}
