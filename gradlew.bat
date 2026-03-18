@ECHO OFF

SET DIR=%~dp0
SET APP_HOME=%DIR%

SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

SET DEFAULT_JVM_OPTS=

java %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

