# ==============================================================================
# AutoDialer — ProGuard Rules
# ==============================================================================

# Keep application class
-keep class com.example.autodialer.AutoDialerApplication { *; }

# ——— Kotlin ——————————————————————————————————————————————————————————————————
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ——— Coroutines ——————————————————————————————————————————————————————————————
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ——— Hilt / Dagger ———————————————————————————————————————————————————————————
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepclasseswithmembers class * {
    @dagger.* <fields>;
    @dagger.* <methods>;
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

# ——— Room ————————————————————————————————————————————————————————————————————
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ——— Retrofit + OkHttp ———————————————————————————————————————————————————————
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# ——— Gson ————————————————————————————————————————————————————————————————————
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep data/model classes used with Gson
-keep class com.example.autodialer.data.model.** { *; }

# ——— Timber ——————————————————————————————————————————————————————————————————
-dontwarn org.jetbrains.annotations.**

# ——— Android / Jetpack ———————————————————————————————————————————————————————
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ——— General —————————————————————————————————————————————————————————————————
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
