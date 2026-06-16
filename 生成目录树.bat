@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo 正在生成详细目录树...

set "output_file=目录树_详细.txt"
echo 当前目录: %cd% > "%output_file%"
echo ======================================== >> "%output_file%"
echo. >> "%output_file%"

call :Tree "%cd%" 0
echo 目录树已生成，保存为 "%output_file%"
pause
exit /b

:Tree
setlocal
set "dir_path=%~1"
set "level=%~2"
set "indent="
for /l %%i in (1,1,!level!) do set "indent=!indent!    "

REM 输出当前目录名
echo !indent!+---%~nx1\ >> "%output_file%"

REM 输出文件
for %%f in ("%dir_path%\*") do (
    if not "%%~nxf"=="%~nx0" (
        echo !indent!    +---%%~nxf >> "%output_file%"
    )
)

REM 递归输出子目录
for /d %%d in ("%dir_path%\*") do (
    call :Tree "%%d" !level! + 1
)
exit /b