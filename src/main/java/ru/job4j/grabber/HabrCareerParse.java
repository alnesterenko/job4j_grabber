package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {

    private static final String SOURCE_LINK = "https://career.habr.com";
    public static final String PREFIX = "/vacancies?page=";
    public static final String SUFFIX = "&q=Java%20developer&type=all";
    public static final int PAGE_LIMITER = 5;

    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

    public DateTimeParser getDateTimeParser() {
        return dateTimeParser;
    }

    public List<Post> list(String link) {
        List<Post> result = new ArrayList<>();
        try {
            for (int i = 1; i <= PAGE_LIMITER; i++) {
                String fullLink = "%s%s%d%s".formatted(SOURCE_LINK, PREFIX, i, SUFFIX);
                Connection connection = Jsoup.connect(fullLink);
                Document document = connection.get();
                Elements rows = document.select(".vacancy-card__inner");
                rows.forEach(row -> {
                    Element titleElement = row.select(".vacancy-card__title").first();
                    Element timeElement = row.select(".basic-date").first();
                    Element linkElement = titleElement.child(0);
                    String vacancyName = titleElement.text();
                    String linkOnOneVacancy = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                    Post tempPost = new Post(vacancyName,
                            linkOnOneVacancy,
                            this.retrieveDescription(linkOnOneVacancy),
                            this.getDateTimeParser().parse(timeElement.attr("datetime")));
                    result.add(tempPost);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     *Метод, который возвращает описание вакансии, но НЕ В ОДНУ СПЛОШНУЮ СТРОКУ,
     * a в удобочитаемом для человека формате -- приблизительно в таком же виде, как это было на странице.
     *
     * @param link to page with one vacancy
     * @return String description 0f vacancy from link
     */
    private String retrieveDescription(String link) {
        Connection connection = Jsoup.connect(link);
        Document document = null;
        try {
            document = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Elements rows = document.select(".faded-content__body");
        return cutOffHTMLClassesFromTags(rows.get(0));
    }

    /**
     * Вырезает теги.
     *
     * @param element -- элемент, весь текст из которого нужно получить.
     * @return тот же самый элемент, без HTML-тегов и в виде String
     */
    private String cutOffHTMLTags(Element element) {
        String tempString = element.toString();
        tempString = tempString.replaceAll(
                "<strong>|</strong>|<ul>|</ul>|<br>|<div.*?>|</div>|<h\\d+?>|<p>|</h\\d+?>|</p>|</li>| {2,4}",
                "");
        tempString = tempString.replaceAll("&nbsp;", " ");
        tempString = tempString.replaceAll("\\s{2,}", System.lineSeparator());
        tempString = tempString.replaceAll("<li>", " - ");
        return tempString;
    }

    /**
     * Задел на будущее. Не вырезает HTML-теги, а только классы у этих тегов,
     * чтобы потом этот элемент можно было разместить на своей HTML-странице
     *
     * @param element -- HTML-элемент, скорее всего div.
     * @return тот же самый элемент, без классов в HTML-тегах и в виде String
     */
    private String cutOffHTMLClassesFromTags(Element element) {
        String tempString = element.toString();
        tempString = tempString.replaceAll(
                " class=\".+?\"",
                "");
        return tempString;
    }
}