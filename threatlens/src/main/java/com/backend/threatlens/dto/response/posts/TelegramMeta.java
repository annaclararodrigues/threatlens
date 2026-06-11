package com.backend.threatlens.dto.response.posts;

import java.util.HashMap;
import java.util.Map;

public record TelegramMeta(long channelId, String channelName, String channelUsername) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("telegram_channel_id", channelId);
        map.put("channel_name", channelName);
        map.put("channel_username", channelUsername);
        return map;
    }
}
