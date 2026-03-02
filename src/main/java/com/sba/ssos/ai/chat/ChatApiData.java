package com.sba.ssos.ai.chat;

import java.util.List;

public record ChatApiData(String input, String output, List<String> sources, List<String> logs) {}
