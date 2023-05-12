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
}
