-keepattributes SourceFile,LineNumberTable
-dontwarn kotlinx.**
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
-keep class com.endroid.fern.widget.** { *; }

-dontwarn androidx.compose.**
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Shizuku — private newProcess is called via reflection; R8 must not strip it
-keep class rikka.shizuku.Shizuku { *; }
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(...);
    public static *** *(...);
}
-keep class rikka.shizuku.ShizukuRemoteProcess { *; }
-keep class rikka.shizuku.ShizukuBinderWrapper { *; }
-keep class rikka.shizuku.SystemServiceHelper { *; }
-keep class moe.shizuku.server.** { *; }
-keepclassmembers class moe.shizuku.server.** { *; }

# Shizuku UserService shell
-keep class com.endroid.fern.shell.** { *; }
-keep interface com.endroid.fern.shell.** { *; }

# Floating overlay service
-keep class com.endroid.fern.overlay.** { *; }

