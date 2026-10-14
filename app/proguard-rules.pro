# -----------------------------------------------------------------------------
# Ximanta GitFetcher — ProGuard / R8 rules
# -----------------------------------------------------------------------------
# Even though isMinifyEnabled = false in build.gradle.kts, this file is still
# referenced by the release build. Keeping it populated is good practice in case
# you later flip minification on (e.g. to shrink the APK).
# -----------------------------------------------------------------------------

# --- OkHttp ---------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep OkHttp public API
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepclassmembers class okhttp3.internal.platform.** { *; }

# --- Kotlin & Coroutines --------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- AndroidX / Compose ---------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# --- Our own model classes (used in JSON parsing via reflection-free code,
# but safe to keep if we ever add Gson/Moshi) ------------------------------
-keep class com.ximanta.gitfetcher.data.** { *; }

# --- Attributes needed for stack traces ------------------------------------
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Serialization / Parcelable safety ------------------------------------
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
