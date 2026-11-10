# Proguard / R8 configuration for Aura Family Safety
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn com.google.android.gms.**
-keepattributes *Annotation*
-keepclassmembers enum * { *; }