@ECHO OFF

IF [%1]==[init] GOTO INIT

java --module-path .bach\cache --module com.github.sormuras.bach %*

GOTO END

:INIT
del .bach\cache\com.github.sormuras.bach@*.jar >nul 2>&1
SETLOCAL
SET tag=%2
IF [%tag%]==[] SET tag=16-ea
jshell -R-Dreboot -R-Dversion=%tag% https://github.com/sormuras/bach/raw/releases/16/init
ENDLOCAL

:END
