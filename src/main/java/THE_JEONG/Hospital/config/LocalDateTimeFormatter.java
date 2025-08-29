package THE_JEONG.Hospital.config;

import org.springframework.format.Formatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LocalDateTimeFormatter implements Formatter<LocalDateTime> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Override
    public LocalDateTime parse(String text, Locale locale) {
        return LocalDateTime.parse(text, formatter);
    }

    @Override
    public String print(LocalDateTime object, Locale locale) {
        return formatter.format(object);
    }
}
