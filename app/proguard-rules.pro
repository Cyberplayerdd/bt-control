# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.btcontrol.app.**$$serializer { *; }
-keepclassmembers class com.btcontrol.app.** { *** Companion; }
-keepclasseswithmembers class com.btcontrol.app.** { kotlinx.serialization.KSerializer serializer(...); }
