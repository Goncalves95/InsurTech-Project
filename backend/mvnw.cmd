@ECHO OFF
SETLOCAL EnableDelayedExpansion

SET "MAVEN_PROJECTBASEDIR=%~dp0"
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
SET "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO Downloading maven-wrapper.jar...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar' -UseBasicParsing"
    IF ERRORLEVEL 1 ( ECHO Failed to download maven-wrapper.jar & EXIT /B 1 )
)

IF NOT "%JAVA_HOME%"=="" (
    SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) ELSE (
    FOR %%i IN (java.exe) DO SET "JAVA_EXE=%%~$PATH:i"
)

IF NOT EXIST "%JAVA_EXE%" (
    ECHO Error: java.exe not found. Set JAVA_HOME or add java to PATH.
    EXIT /B 1
)

SET "MAVEN_OPTS=%MAVEN_OPTS% --enable-native-access=ALL-UNNAMED"

"%JAVA_EXE%" %MAVEN_OPTS% ^
    -classpath "%WRAPPER_JAR%" ^
    "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR:~0,-1%" ^
    %WRAPPER_LAUNCHER% %*

EXIT /B %ERRORLEVEL%
