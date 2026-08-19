package dev.strace.twings.util;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color formatting for legacy ({@code &a}) and hex ({@code #ff0000}) codes.
 * Hex support used to be gated on a broken version sniff ("1.16"/"1.17");
 * every supported server understands hex now, so it is always on.
 */
public final class MyColors {

    private static final Pattern HEX = Pattern.compile("#[a-fA-F0-9]{6}");

    private MyColors() {
    }

    public static String format(String msg) {
        if (msg == null) return "";
        if (msg.indexOf('#') >= 0) {
            Matcher match = HEX.matcher(msg);
            StringBuilder out = new StringBuilder();
            while (match.find()) {
                match.appendReplacement(out, Matcher.quoteReplacement(ChatColor.of(match.group()).toString()));
            }
            match.appendTail(out);
            msg = out.toString();
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /** Strips both legacy and hex color codes, e.g. for name matching. */
    public static String strip(String msg) {
        if (msg == null) return "";
        String formatted = format(msg);
        String stripped = ChatColor.stripColor(formatted);
        return stripped == null ? "" : stripped;
    }
}
