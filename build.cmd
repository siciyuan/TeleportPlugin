@echo off
setlocal enabledelayedexpansion

set JAVA_HOME=D:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%

set CLASSPATH=
for /r "D:\Users\Administrator\Desktop\minecraft\MSL\Server\libraries" %%a in (*.jar) do (
    set "CLASSPATH=!CLASSPATH!;%%a"
)

echo Compiling TeleportPlugin v2.1.0...

rmdir /s /q target 2>nul
mkdir target\classes

javac -cp "%CLASSPATH%" -d target/classes src/main/java/com/scroam/teleport/*.java

if %errorlevel% equ 0 (
    echo Compilation successful!
    jar cf target/TeleportPlugin-2.1.0.jar -C target/classes . -C src/main/resources .
    echo JAR created at target/TeleportPlugin-2.1.0.jar
    xcopy /Y "target\TeleportPlugin-2.1.0.jar" "D:\Users\Administrator\Desktop\minecraft\MSL\Server\plugins\"
) else (
    echo Compilation failed!
    pause
)
