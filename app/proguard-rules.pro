# ── NutriLens ProGuard / R8 rules ────────────────────────────────────────────

# Keep all app classes (data models, Room entities, Gson-serialized objects)
-keep class com.factory.nutrilensaimacrotracker.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# ── Gson ──────────────────────────────────────────────────────────────────────
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ── OkHttp / Okio ─────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Google Play Billing ────────────────────────────────────────────────────────
-keep class com.android.billingclient.** { *; }
-keep interface com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# ── Kotlin Coroutines ──────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Kotlin serialization ───────────────────────────────────────────────────────
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.Metadata { *; }

# ── AndroidX / Jetpack ────────────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.window.**

# ── Room ──────────────────────────────────────────────────────────────────────
# Room's generated code is kept by the @Entity / @Dao annotations; the rule
# above ("-keep class com.factory...") already covers data model classes.
-dontwarn androidx.room.**

# ── Compose ───────────────────────────────────────────────────────────────────
# Compose uses reflection for Composable introspection in some paths.
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── SplashScreen ──────────────────────────────────────────────────────────────
-keep class androidx.core.splashscreen.** { *; }

# ── General Android safety ────────────────────────────────────────────────────
-keepclassmembers class * extends android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
