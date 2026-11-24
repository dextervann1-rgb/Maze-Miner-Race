# Maze Miner Race — Release R8 / ProGuard Keep Rules
# © 2026 Vann Family Ventures LLC. All rights reserved.

# Preserve stack trace readability for Play Console crash diagnostics
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Room Database Entities, DAOs, and generated implementations
-keep class com.example.data.**Entity { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Retrofit & Moshi JSON Serialization DTOs (Replay Verification & Play Billing Verification)
-keep class com.example.data.*Dto { *; }
-keep class com.example.data.*DtoJsonAdapter { *; }
-keep interface com.example.data.VerifierApiService { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Google Play Billing Library v7.1.1 & App Storefront Specs
-keep class com.android.billingclient.api.** { *; }
-keep class com.example.billing.** { *; }

# Deterministic Engine Models & Replay Action Payloads
-keep class com.example.engine.** { *; }

# Kotlinx Serialization & Coroutines safety
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    public static kotlinx.serialization.KSerializer serializer(...);
}
