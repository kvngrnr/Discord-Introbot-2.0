package com.jagrosh.jmusicbot;

public class IntroConfig {
    private String link;
    private Integer seek;

    public IntroConfig(String link, Integer seek)
    {
        this.link = link;
        this.seek = seek;
    }

    public String getLink() {
        return link;
    }

    public Integer getSeek() {
        return seek;
    }
}
