package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.List;

public class HabrCareerParse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    public static final int PAGE_LIMITER = 1; /*Не забыть вернуть назад 5*/

    private String retrieveDescription(String link) throws IOException {
        Connection connection = Jsoup.connect(link);
        Document document = connection.get();
        Elements rows = document.select(".faded-content__body");
        return cutterOfHTMLTags(rows.get(0));
    }

    private String cutterOfHTMLTags(Element element) {
        String tempString = element.toString();
        tempString = tempString.replaceAll(
                "<strong>|</strong>|<ul>|</ul>|<br>|<div.*?>|</div>|<h\\d+?>|<p>|</h\\d+?>|</p>|</li>| {2,4}",
                "");
        tempString = tempString.replaceAll("&nbsp;", " ");
        tempString = tempString.replaceAll("\\s{2,}", System.lineSeparator());
        tempString = tempString.replaceAll("<li>", " - ");
        return tempString;
    }

    public static void main(String[] args) throws IOException {
        for (int i = 1; i <= PAGE_LIMITER; i++) {
            String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, i, SUFFIX);
            DateTimeParser formatter = new HabrCareerDateTimeParser();
            Connection connection = Jsoup.connect(fullLink);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element timeElement = row.select(".basic-date").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                System.out.printf("%s %s date: %s%n", vacancyName, link,
                        formatter.parse(timeElement.attr("datetime")));
            });
        }
        HabrCareerParse testParse = new HabrCareerParse();
        System.out.println(testParse.retrieveDescription("https://career.habr.com/vacancies/1000139737"));
    }
}