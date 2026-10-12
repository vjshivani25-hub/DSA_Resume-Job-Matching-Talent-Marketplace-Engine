@echo off
title TalentMatch DSA Project
if not exist out mkdir out
del /q out\*.class >nul 2>&1
for /R src %%f in (*.java) do echo %%f>>sources.txt
javac -d out @sources.txt
if errorlevel 1 (
  echo.
  echo Compilation failed.
  pause
  exit /b 1
)
del sources.txt
echo.
echo Starting TalentMatch...
echo Open http://localhost:8080
echo.
java -cp out Main
pause
