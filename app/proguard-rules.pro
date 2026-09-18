-keepattributes SourceFile,LineNumberTable
-dontwarn kotlinx.**
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-keep class com.endroid.fern.widget.** { *; }

# Size: strip more logging
-assumenosideeffects class android.util.Log {
    public static *** w(...);
}
# Compose / Kotlin
-dontwarn androidx.compose.**
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
