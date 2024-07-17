package ru.job4j.grabber.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class HabrCareerDateTimeParserTest {

    @Test
    public void approximatelyNow() {
        String date = "2024-07-11T18:27:06+03:00";
        DateTimeParser newParser = new HabrCareerDateTimeParser();
        LocalDateTime result = newParser.parse(date);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 7, 11, 18, 27, 6));
    }

    @Test
    public void firstCrusade() {
        String date = "1096-09-01T18:00:00+00:00";
        DateTimeParser newParser = new HabrCareerDateTimeParser();
        LocalDateTime result = newParser.parse(date);
        assertThat(result).isEqualTo(LocalDateTime.of(1096, 9, 1, 18, 0, 0));
    }
}