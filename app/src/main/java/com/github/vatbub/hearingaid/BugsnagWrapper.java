package com.github.vatbub.hearingaid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Map;

/**
 * Stub implementation of BugsnagWrapper to remove dependency on Bugsnag.
 */
public class BugsnagWrapper {

    public static void setAppVersion(final String appVersion) {}

    @Nullable
    public static String getContext() { return null; }

    public static void setContext(final String context) {}

    public static void setBuildUUID(final String buildUuid) {}

    public static void setFilters(final String... filters) {}

    public static void setIgnoreClasses(final String... ignoreClasses) {}

    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {}

    public static void setProjectPackages(final String... projectPackages) {}

    public static void setReleaseStage(final String releaseStage) {}

    public static void setSendThreads(final boolean sendThreads) {}

    public static void setAutoCaptureSessions(boolean autoCapture) {}

    public static void setUser(final String id, final String email, final String name) {}

    public static void clearUser() {}

    public static void setUserId(final String id) {}

    public static void setUserEmail(final String email) {}

    public static void setUserName(final String name) {}

    public static void beforeNotify(final Object beforeNotify) {}

    public static void beforeRecordBreadcrumb(final Object beforeRecordBreadcrumb) {}

    public static void notify(@NonNull final Throwable exception) {
        exception.printStackTrace();
    }

    public static void notify(@NonNull final Throwable exception, final Object callback) {
        exception.printStackTrace();
    }

    public static void notify(@NonNull String name,
                              @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace,
                              Object callback) {}

    public static void notify(@NonNull final Throwable exception, final Severity severity) {
        exception.printStackTrace();
    }

    public static void notify(@NonNull final Throwable exception,
                              @NonNull final Map<String, Object> metaData) {
        exception.printStackTrace();
    }

    public static void notify(@NonNull final Throwable exception, final Severity severity,
                              @NonNull final Map<String, Object> metaData) {
        exception.printStackTrace();
    }

    public static void addToTab(final String tab, final String key, final Object value) {}

    public static void clearTab(String tabName) {}

    @Nullable
    public static Object getMetaData() { return null; }

    public static void setMetaData(@NonNull final Object metaData) {}

    public static void leaveBreadcrumb(@NonNull String message) {}

    public static void leaveBreadcrumb(@NonNull String name,
                                       @NonNull Object type,
                                       @NonNull Map<String, String> metadata) {}

    public static void clearBreadcrumbs() {}

    public static void enableExceptionHandler() {}

    public static void disableExceptionHandler() {}

    public static void setLoggingEnabled(boolean enabled) {}

    public static void startSession() {}

    @Nullable
    public static Object getClient() { return null; }

    public enum Severity {
        INFO, WARNING, ERROR
    }
}
