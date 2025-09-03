# Networking/JSON
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class org.json.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep our classes & service names
-keep class com.bhavya.smsrelay.** { *; }
-keepnames class com.bhavya.smsrelay.SmsNotificationListener
