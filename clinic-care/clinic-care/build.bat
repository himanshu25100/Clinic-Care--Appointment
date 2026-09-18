@echo off
REM Compiles the application and the tests into out\
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java test\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
echo Build OK -^> out\
echo Run app  : java -cp out com.clinic.Main
echo Run tests: java -cp out com.clinic.test.SelfTest
