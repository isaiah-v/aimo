package org.ivcode.aimo.ollama

import org.springframework.ai.ollama.api.OllamaChatOptions

internal fun applyRawOptions(builder: OllamaChatOptions.Builder<*>, options: Map<String, Any>) {
    options.forEach { (key, value) ->
        when (key.normalize()) {
            "model"            -> builder.model(value.asString())
            "numa"             -> builder.useNUMA(value.asBoolean())
            "numctx"           -> builder.numCtx(value.asInt())
            "numbatch"         -> builder.numBatch(value.asInt())
            "numgpu"           -> builder.numGPU(value.asInt())
            "maingpu"          -> builder.mainGPU(value.asInt())
            "lowvram"          -> builder.lowVRAM(value.asBoolean())
            "f16kv"            -> builder.f16KV(value.asBoolean())
            "logitsall"        -> builder.logitsAll(value.asBoolean())
            "vocabonly"        -> builder.vocabOnly(value.asBoolean())
            "usemmap"          -> builder.useMMap(value.asBoolean())
            "usemlock"         -> builder.useMLock(value.asBoolean())
            "numthread"        -> builder.numThread(value.asInt())
            "numkeep"          -> builder.numKeep(value.asInt())
            "seed"             -> builder.seed(value.asInt())
            "numpredict",
            "maxtokens"        -> builder.maxTokens(value.asInt())
            "topk"             -> builder.topK(value.asInt())
            "topp"             -> builder.topP(value.asDouble())
            "minp"             -> builder.minP(value.asDouble())
            "tfsz"             -> builder.tfsZ(value.asFloat())
            "typicalp"         -> builder.typicalP(value.asFloat())
            "repeatlastn"      -> builder.repeatLastN(value.asInt())
            "temperature"      -> builder.temperature(value.asDouble())
            "repeatpenalty"    -> builder.repeatPenalty(value.asDouble())
            "presencepenalty"  -> builder.presencePenalty(value.asDouble())
            "frequencypenalty" -> builder.frequencyPenalty(value.asDouble())
            "mirostat"         -> builder.mirostat(value.asInt())
            "mirostattau"      -> builder.mirostatTau(value.asFloat())
            "mirostateta"      -> builder.mirostatEta(value.asFloat())
            "penalizenewline"  -> builder.penalizeNewline(value.asBoolean())
            "stop"             -> builder.stopSequences(value.asStringList())
            "format"           -> builder.format(value)
            "keepalive"        -> builder.keepAlive(value.asString())
            "truncate"         -> builder.truncate(value.asBoolean())
            else               -> throw IllegalArgumentException("Unsupported Ollama option '$key'")
        }
    }
}

private fun String.normalize() = lowercase().replace("-", "").replace("_", "")

private fun Any.asString() = toString()
private fun Any.asBoolean() = when (this) {
    is Boolean -> this
    else -> toString().toBooleanStrict()
}
private fun Any.asInt() = when (this) {
    is Number -> toInt()
    else -> toString().toInt()
}
private fun Any.asDouble() = when (this) {
    is Number -> toDouble()
    else -> toString().toDouble()
}
private fun Any.asFloat() = when (this) {
    is Number -> toFloat()
    else -> toString().toFloat()
}
@Suppress("UNCHECKED_CAST")
private fun Any.asStringList(): List<String> = when (this) {
    is List<*> -> this as List<String>
    else -> toString().split(",").map { it.trim() }
}
