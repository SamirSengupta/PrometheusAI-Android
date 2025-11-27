package com.samcodeman

sealed class PromptIntent {
    // If systemPrompt is null, PromptBuilder will fallback to ModelConfig.defaultSystemPrompt
    data class QA(val systemPrompt: String? = null) : PromptIntent()
}