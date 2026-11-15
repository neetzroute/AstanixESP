package dev.astanix.esp.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;

/**
 * Text utility for MiniMessage formatting.
 */
public final class TextUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private TextUtil() {}

    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static Component parse(String text, Map<String, String> placeholders) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        String replaced = text;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return parse(replaced);
    }

    public static Component parse(String text, TagResolver... tagResolvers) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(text, tagResolvers)
                .decoration(TextDecoration.ITALIC, false);
    }
}
