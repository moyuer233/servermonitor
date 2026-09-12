# Keep JSch and Gson models for reflection
-keep class com.jcraft.jsch.** { *; }
-keep class com.servermonitor.model.** { *; }
-dontwarn com.jcraft.jsch.**