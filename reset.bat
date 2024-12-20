@echo off
setlocal

rem Remove all directories with .gradle...
for /d /r %%G in (*.gradle) do (
    echo Remove directory: %%G
    rd /s /q "%%G"
)

echo Done!
endlocal
