package io.github.ieswar23.strikezone.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Formatters {

    private Formatters() {
    }

    public static String matchDate(long epochMillis) {
        return new SimpleDateFormat("d MMM yyyy · h:mm a", Locale.getDefault()).format(new Date(epochMillis));
    }

    /** Compact date for dense rows, e.g. "24 Sep 2026". */
    public static String shortDate(long epochMillis) {
        return new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(new Date(epochMillis));
    }

    public static String todayLabel() {
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
    }

    public static int dayOfYear() {
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }

    /** "ATK" style initials for a badge, e.g. "Mumbai Mavericks" → "MM". */
    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() < 2 && !p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        }
        return sb.toString();
    }
}
