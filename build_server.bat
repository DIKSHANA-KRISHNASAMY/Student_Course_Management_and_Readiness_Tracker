@echo off
setlocal

echo Cleaning up...
if exist out rd /s /q out
mkdir out

echo Compiling Java Sources...
javac -cp "lib/*;src/main/java" -d out ^
 src\main\java\AppServer.java ^
 src\main\java\model\*.java ^
 src\main\java\dao\*.java ^
 src\main\java\util\*.java ^
 src\main\java\handlers\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation Failed!
    exit /b %ERRORLEVEL%
)

echo Build Successful!
echo To run: java -cp "out;lib/*" AppServer
endlocal
