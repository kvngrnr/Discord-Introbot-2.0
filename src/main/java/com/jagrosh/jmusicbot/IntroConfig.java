package com.jagrosh.jmusicbot;

public class IntroConfig {
    private String link;
    private Integer seek;
    private String title;

    public IntroConfig(String link, Integer seek, String title)
    {
        this.link = link;
        this.seek = seek;
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public Integer getSeek() {
        return seek;
    }

    public String getTitle() {
        return title;
    }

    public static String toString(IntroConfig config)
    {
        return String.format("{ link: \"%s\", seek: %d, title: \"%s\" },", config.link, config.seek, config.title);
    }

    public static String arrayToString(IntroConfig[] config)
    {
        String s = "[" + System.lineSeparator();
        for (IntroConfig introConfig : config) {
            s = s.concat(toString(introConfig) + System.lineSeparator());
        }
        s = s.concat("];");
        return s;
    }
}
