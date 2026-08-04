package com.ai.applications.rag.insightvault.models;

import java.util.List;

public record ChatAnswer(String answer, List<String> sources) {
}
