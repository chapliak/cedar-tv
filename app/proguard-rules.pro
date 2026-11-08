# Cedar TV R8 rules (libraries ship their own consumer rules; these cover app data classes read by kotlinx.serialization)
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class app.cedartv.**$$serializer { *; }
-keepclassmembers class app.cedartv.** { *** Companion; }
-keepclasseswithmembers class app.cedartv.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
