package kz.qorgau.scamguardian.data.rules

import android.content.Context
import kotlinx.serialization.json.Json
import kz.qorgau.scamguardian.domain.model.RulePack
import java.io.InputStream

/**
 * Loads versioned JSON rule packs from assets (or any stream).
 * Pure parsing is testable without Android when using [loadFromString].
 */
class RulePackLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadFromAssets(
        context: Context,
        assetPath: String = DEFAULT_ASSET_PATH,
    ): RulePack {
        context.assets.open(assetPath).use { stream ->
            return loadFromStream(stream)
        }
    }

    fun loadFromStream(stream: InputStream): RulePack {
        val text = stream.bufferedReader(Charsets.UTF_8).readText()
        return loadFromString(text)
    }

    fun loadFromString(rawJson: String): RulePack {
        val dto = json.decodeFromString(RulePackDto.serializer(), rawJson)
        require(dto.rules.isNotEmpty()) { "Rule pack has no rules" }
        dto.rules.forEach { rule ->
            require(rule.id.isNotBlank()) { "Rule id must not be blank" }
            require(rule.patterns.isNotEmpty()) { "Rule ${rule.id} has no patterns" }
            require(rule.severityWeight > 0f) { "Rule ${rule.id} weight must be > 0" }
        }
        return dto.toDomain()
    }

    companion object {
        const val DEFAULT_ASSET_PATH: String = "rules/default_rules_v1.json"
    }
}
