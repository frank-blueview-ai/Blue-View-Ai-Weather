# Keep serializable DTOs
-keep class ai.blueview.weather.data.api.dto.** { *; }
-keepattributes *Annotation*

# kotlinx-serialization
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Hilt
-dontwarn dagger.hilt.**
