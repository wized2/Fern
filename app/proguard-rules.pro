-keepattributes SourceFile,LineNumberTable
-dontwarn kotlinx.**
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
-keep class com.endroid.fern.widget.** { *; }
