package sk.uniza.adamec2.util;

public class TimeParser {
    /**
     * Vygenerované pomocou AI, v dokumentácií kapitola 2.b)
     */
    public static String parseTime(double time) {
        long totalSeconds = Math.round(time * 3600);
        int hours   = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Vygenerované pomocou AI, v dokumentácií kapitola 2.a)
     */
    public static double parseTime(String text) {
        String trimmed = text.trim();
        String[] hms = trimmed.split(":");
        if (hms.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid time format: \"" + text + "\". Use H:MM:SS (e.g. 6:30:15)"
            );
        }

        int hours;
        int minutes;
        int seconds;
        try {
            hours = Integer.parseInt(hms[0].trim());
            minutes = Integer.parseInt(hms[1].trim());
            seconds = Integer.parseInt(hms[2].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid time format: \"" + text + "\". Use H:MM:SS (e.g. 6:30:15)"
            );
        }

        if (hours < 0 || hours > 100000) {
            throw new IllegalArgumentException("Hours must be between 0 and 23");
        }
        if (minutes < 0 || minutes > 59) {
            throw new IllegalArgumentException("Minutes must be between 0 and 59");
        }
        if (seconds < 0 || seconds > 59) {
            throw new IllegalArgumentException("Seconds must be between 0 and 59");
        }

        return hours + minutes / 60.0 + seconds / 3600.0;
    }

}
