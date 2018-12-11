package com.github.vatbub.hearingaid;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.BeforeRecordBreadcrumb;
import com.bugsnag.android.BreadcrumbType;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Callback;
import com.bugsnag.android.Client;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.MetaData;
import com.bugsnag.android.Severity;

import java.util.Map;

/**
 * Wrapper for the {@code Bugsnag} class but methods become no-ops if Bugsnag is not initialized (instead of throwing an exception)
 */
public class BugsnagWrapper {
    /**
     * Set the application version sent to Bugsnag. By default we'll pull this
     * from your AndroidManifest.xml
     *
     * @param appVersion the app version to send
     */
    public static void setAppVersion(final String appVersion) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setAppVersion(appVersion);
    }

    /**
     * Gets the context to be sent to Bugsnag.
     *
     * @return Context or {@code null} if Bugsnag is not initialized
     */
    @Nullable
    public static String getContext() {
        if (!CustomApplication.isBugSnagInitialized()) return null;
        return Bugsnag.getContext();
    }

    /**
     * Set the context sent to Bugsnag. By default we'll attempt to detect the
     * name of the top-most activity at the time of a report, and use this
     * as the context, but sometime this is not possible.
     *
     * @param context set what was happening at the time of a crash
     */
    public static void setContext(final String context) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setContext(context);
    }

    /**
     * Set the buildUUID to your own value. This is used to identify proguard
     * mapping files in the case that you publish multiple different apps with
     * the same appId and versionCode. The default value is read from the
     * com.bugsnag.android.BUILD_UUID meta-data field in your app manifest.
     *
     * @param buildUuid the buildUuid.
     */
    public static void setBuildUUID(final String buildUuid) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setBuildUUID(buildUuid);
    }

    /**
     * Set which keys should be filtered when sending metaData to Bugsnag.
     * Use this when you want to ensure sensitive information, such as passwords
     * or credit card information is stripped from metaData you send to Bugsnag.
     * Any keys in metaData which contain these strings will be marked as
     * [FILTERED] when send to Bugsnag.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setFilters("password", "credit_card");
     *
     * @param filters a list of keys to filter from metaData
     */
    public static void setFilters(final String... filters) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setFilters(filters);
    }

    /**
     * Set which exception classes should be ignored (not sent) by Bugsnag.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setIgnoreClasses("java.lang.RuntimeException");
     *
     * @param ignoreClasses a list of exception classes to ignore
     */
    public static void setIgnoreClasses(final String... ignoreClasses) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setIgnoreClasses(ignoreClasses);
    }

    /**
     * Set for which releaseStages errors should be sent to Bugsnag.
     * Use this to stop errors from development builds being sent.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setNotifyReleaseStages("production");
     *
     * @param notifyReleaseStages a list of releaseStages to notify for
     * @see #setReleaseStage
     */
    public static void setNotifyReleaseStages(final String... notifyReleaseStages) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setNotifyReleaseStages(notifyReleaseStages);
    }

    /**
     * Set which packages should be considered part of your application.
     * Bugsnag uses this to help with error grouping, and stacktrace display.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setProjectPackages("com.example.myapp");
     * <p>
     * By default, we'll mark the current package name as part of you app.
     *
     * @param projectPackages a list of package names
     */
    public static void setProjectPackages(final String... projectPackages) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setProjectPackages(projectPackages);
    }

    /**
     * Set the current "release stage" of your application.
     * By default, we'll set this to "development" for debug builds and
     * "production" for non-debug builds.
     * <p>
     * If the release stage is set to "production", logging will automatically be disabled.
     *
     * @param releaseStage the release stage of the app
     * @see #setNotifyReleaseStages {@link #setLoggingEnabled(boolean)}
     */
    public static void setReleaseStage(final String releaseStage) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setReleaseStage(releaseStage);
    }

    /**
     * Set whether to send thread-state with report.
     * By default, this will be true.
     *
     * @param sendThreads should we send thread-state with report?
     */
    public static void setSendThreads(final boolean sendThreads) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setSendThreads(sendThreads);
    }

    /**
     * Sets whether or not Bugsnag should automatically capture and report User sessions whenever
     * the app enters the foreground.
     * <p>
     * By default this behavior is disabled.
     *
     * @param autoCapture whether sessions should be captured automatically
     */
    public static void setAutoCaptureSessions(boolean autoCapture) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setAutoCaptureSessions(autoCapture);
    }

    /**
     * Set details of the user currently using your application.
     * You can search for this information in your Bugsnag dashboard.
     * <p>
     * For example:
     * <p>
     * Bugsnag.setUser("12345", "james@example.com", "James Smith");
     *
     * @param id    a unique identifier of the current user (defaults to a unique id)
     * @param email the email address of the current user
     * @param name  the name of the current user
     */
    public static void setUser(final String id, final String email, final String name) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setUser(id, email, name);
    }

    /**
     * Removes the current user data and sets it back to defaults
     */
    public static void clearUser() {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.clearUser();
    }

    /**
     * Set a unique identifier for the user currently using your application.
     * By default, this will be an automatically generated unique id
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param id a unique identifier of the current user
     */
    public static void setUserId(final String id) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setUserId(id);
    }

    /**
     * Set the email address of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param email the email address of the current user
     */
    public static void setUserEmail(final String email) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setUserEmail(email);
    }

    /**
     * Set the name of the current user.
     * You can search for this information in your Bugsnag dashboard.
     *
     * @param name the name of the current user
     */
    public static void setUserName(final String name) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setUserName(name);
    }

    /**
     * Add a "before notify" callback, to execute code before sending
     * reports to Bugsnag.
     * <p>
     * You can use this to add or modify information attached to an error
     * before it is sent to your dashboard. You can also return
     * <code>false</code> from any callback to prevent delivery. "Before
     * notify" callbacks do not run before reports generated in the event
     * of immediate app termination from crashes in C/C++ code.
     * <p>
     * For example:
     * <p>
     * Bugsnag.beforeNotify(new BeforeNotify() {
     * public boolean run(Error error) {
     * error.setSeverity(Severity.INFO);
     * return true;
     * }
     * })
     *
     * @param beforeNotify a callback to run before sending errors to Bugsnag
     * @see BeforeNotify
     */
    public static void beforeNotify(final BeforeNotify beforeNotify) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.beforeNotify(beforeNotify);
    }

    /**
     * Add a "before breadcrumb" callback, to execute code before every
     * breadcrumb captured by Bugsnag.
     * <p>
     * You can use this to modify breadcrumbs before they are stored by Bugsnag.
     * You can also return <code>false</code> from any callback to ignore a breadcrumb.
     * <p>
     * For example:
     * <p>
     * Bugsnag.beforeRecordBreadcrumb(new BeforeRecordBreadcrumb() {
     * public boolean shouldRecord(Breadcrumb breadcrumb) {
     * return false; // ignore the breadcrumb
     * }
     * })
     *
     * @param beforeRecordBreadcrumb a callback to run before a breadcrumb is captured
     * @see BeforeRecordBreadcrumb
     */
    public static void beforeRecordBreadcrumb(final BeforeRecordBreadcrumb beforeRecordBreadcrumb) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.beforeRecordBreadcrumb(beforeRecordBreadcrumb);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     */
    public static void notify(@NonNull final Throwable exception) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(exception);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param callback  callback invoked on the generated error report for
     *                  additional modification
     */
    public static void notify(@NonNull final Throwable exception, final Callback callback) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(exception, callback);
    }

    /**
     * Notify Bugsnag of an error
     *
     * @param name       the error name or class
     * @param message    the error message
     * @param stacktrace the stackframes associated with the error
     * @param callback   callback invoked on the generated error report for
     *                   additional modification
     */
    public static void notify(@NonNull String name,
                              @NonNull String message,
                              @NonNull StackTraceElement[] stacktrace,
                              Callback callback) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(name, message, stacktrace, callback);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     */
    public static void notify(@NonNull final Throwable exception, final Severity severity) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(exception, severity);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public static void notify(@NonNull final Throwable exception,
                              @NonNull final MetaData metaData) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(exception, metaData);
    }

    /**
     * Notify Bugsnag of a handled exception
     *
     * @param exception the exception to send to Bugsnag
     * @param severity  the severity of the error, one of Severity.ERROR,
     *                  Severity.WARNING or Severity.INFO
     * @param metaData  additional information to send with the exception
     * @deprecated Use {@link #notify(Throwable, Callback)} to send and modify error reports
     */
    @Deprecated
    public static void notify(@NonNull final Throwable exception, final Severity severity,
                              @NonNull final MetaData metaData) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.notify(exception, severity, metaData);
    }

    /**
     * Add diagnostic information to every error report.
     * Diagnostic information is collected in "tabs" on your dashboard.
     * <p>
     * For example:
     * <p>
     * Bugsnag.addToTab("account", "name", "Acme Co.");
     * Bugsnag.addToTab("account", "payingCustomer", true);
     *
     * @param tab   the dashboard tab to add diagnostic data to
     * @param key   the name of the diagnostic information
     * @param value the contents of the diagnostic information
     */
    public static void addToTab(final String tab, final String key, final Object value) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.addToTab(tab, key, value);
    }

    /**
     * Remove a tab of app-wide diagnostic information
     *
     * @param tabName the dashboard tab to remove diagnostic data from
     */
    public static void clearTab(String tabName) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.clearTab(tabName);
    }

    /**
     * Get the global diagnostic information currently stored in MetaData.
     *
     * @return The metadata or {@code null} if Bugsnag was not initialized
     * @see MetaData
     */
    @Nullable
    public static MetaData getMetaData() {
        if (!CustomApplication.isBugSnagInitialized()) return null;
        return Bugsnag.getMetaData();
    }

    /**
     * Set the global diagnostic information to be send with every error.
     *
     * @see MetaData
     */
    public static void setMetaData(@NonNull final MetaData metaData) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setMetaData(metaData);
    }

    /**
     * Leave a "breadcrumb" log message, representing an action that occurred
     * in your app, to aid with debugging.
     *
     * @param message the log message to leave (max 140 chars)
     */
    public static void leaveBreadcrumb(@NonNull String message) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.leaveBreadcrumb(message);
    }

    /**
     * Leave a "breadcrumb" log message representing an action or event which
     * occurred in your app, to aid with debugging
     *
     * @param name     A short label (max 32 chars)
     * @param type     A category for the breadcrumb
     * @param metadata Additional diagnostic information about the app environment
     */
    public static void leaveBreadcrumb(@NonNull String name,
                                       @NonNull BreadcrumbType type,
                                       @NonNull Map<String, String> metadata) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.leaveBreadcrumb(name, type, metadata);
    }

    /**
     * Clear any breadcrumbs that have been left so far.
     */
    public static void clearBreadcrumbs() {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.clearBreadcrumbs();
    }

    /**
     * Enable automatic reporting of unhandled exceptions.
     * By default, this is automatically enabled in the constructor.
     */
    public static void enableExceptionHandler() {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.enableExceptionHandler();
    }

    /**
     * Disable automatic reporting of unhandled exceptions.
     */
    public static void disableExceptionHandler() {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.disableExceptionHandler();
    }

    /**
     * Sets whether the SDK should write logs. In production apps, it is recommended that this
     * should be set to false.
     * <p>
     * Logging is enabled by default unless the release stage is set to 'production', in which case
     * it will be disabled.
     *
     * @param enabled true if logging is enabled
     */
    public static void setLoggingEnabled(boolean enabled) {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.setLoggingEnabled(enabled);
    }

    /**
     * Manually starts tracking a new session.
     * <p>
     * Automatic session tracking can be enabled via
     * {@link Configuration#setAutoCaptureSessions(boolean)}, which will automatically create a new
     * session everytime the app enters the foreground.
     */
    public static void startSession() {
        if (!CustomApplication.isBugSnagInitialized()) return;
        Bugsnag.startSession();
    }

    /**
     * Get the current Bugsnag Client instance.
     *
     * @return The bugsnag client or {@code null if bugsnag is not initialized}
     */
    @Nullable
    public static Client getClient() {
        if (!CustomApplication.isBugSnagInitialized()) return null;
        return Bugsnag.getClient();
    }
}
