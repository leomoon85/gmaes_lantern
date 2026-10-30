# Save schema names must remain stable across app updates.
-keep class com.lantern.dispatch.Game { *; }
-keep class com.lantern.dispatch.Game$* { *; }
-keep class com.lantern.dispatch.SaveCodec { *; }
-keepattributes Signature,InnerClasses,EnclosingMethod
