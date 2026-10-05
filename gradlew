#!/bin/sh

#
# Gradle wrapper startup script for POSIX
#

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Increase max memory
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Execute Gradle
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
