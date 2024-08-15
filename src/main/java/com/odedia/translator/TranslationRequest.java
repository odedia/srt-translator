package com.odedia.translator;

public record TranslationRequest(
    String sourceFile,
    String sourceLanguage,
    String targetFile,
    String targetLanguage) {
}
