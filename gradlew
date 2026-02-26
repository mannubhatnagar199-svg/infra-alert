#!/bin/sh
##
## Gradle start up script for UN*X
##
APP_HOME="$(cd "$(dirname "$0")" && pwd)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME}/bin/java"
if [ -z "$JAVA_HOME" ]; then
    JAVACMD="java"
fi
exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
