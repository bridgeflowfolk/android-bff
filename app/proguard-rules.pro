# Retrofit + Gson : conserver les DTOs
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.bridgeflowfolk.bff.data.remote.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
