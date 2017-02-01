# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/android-sdk-macosx/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep public class org.dwallach.** { *; }
-keep public class kotlin.reflect.** { *; }

-dontwarn kotlin.**

# painful hack per http://stackoverflow.com/questions/23883028/how-to-fix-proguard-warning-cant-find-referenced-method-for-existing-methods
# which, naturally, doesn't even solve the problem
# -keepnames class ** { *; }
# -keepnames interface ** { *; }
# -keepnames enum ** { *; }

# hackery to work arouond version mismatch issues between Anko and the Android library
# See, e.g.,: https://github.com/stephanenicolas/robospice/issues/437
-dontwarn org.jetbrains.anko.internals.AnkoInternals

-dontobfuscate
