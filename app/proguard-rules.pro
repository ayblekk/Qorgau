# ScamGuardian Stage 1 — keep Room entities and rule models readable for audit.
-keep class kz.qorgau.scamguardian.data.local.db.entity.** { *; }
-keep class kz.qorgau.scamguardian.domain.model.** { *; }

# Do not strip useful crash stack for domain layer (no message content is logged).
-keepattributes SourceFile,LineNumberTable
