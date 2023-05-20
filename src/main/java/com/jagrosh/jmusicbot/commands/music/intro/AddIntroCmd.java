/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.commands.music.intro;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.IntroConfig;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import net.dv8tion.jda.api.entities.Message;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AddIntroCmd extends MusicCommand {

    private final String loadingEmoji;
    static volatile Integer seek; // idk, java sucks

    public AddIntroCmd(Bot bot) {
        super(bot);
        this.name = "addintro";
        this.loadingEmoji = bot.getConfig().getLoading();
        this.help = "add an intro for this user";
        this.arguments = "<link title seek>";
        this.aliases = bot.getConfig().getAliases(this.name);
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.replyError("Please include the link of the intro and a title (and optionally a seek)");
            return;
        }
        String[] argList = event.getArgs().split(" ");
        String link = argList[0];
        if (!link.matches(
                "http(?:s?):\\/\\/(?:www\\.)?youtu(?:be\\.com\\/watch\\?v=|\\.be\\/)([\\w\\-\\_]*)(&(amp;)?‌​[\\w\\?‌​=]*)?")) {
            event.replyError("No valid youtube link provided");
            return;
        }

        String seekString = argList.length > 1 ? argList[1] : "0";
        seek = 0;
        try {
            seek = Integer.parseInt(seekString);
            if (seek < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            event.replyError("Provided seek is not a valid number");
            return;
        }

        event.reply(loadingEmoji + " Loading... `[" + link + "]`",
                m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), link, new ResultHandler(m, event, seek)));
    }

    private class ResultHandler implements AudioLoadResultHandler {
        private final Message m;
        private final CommandEvent event;
        private final Integer seek;

        private ResultHandler(Message m, CommandEvent event, Integer seek) {
            this.m = m;
            this.event = event;
            this.seek = seek;
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            AudioTrackInfo info = track.getInfo();

            Long userId = event.getMember().getIdLong();
            String userName = event.getMember().getUser().getName();
            boolean success = bot.getConfig().addIntro(userId, userName, new IntroConfig(info.uri, seek, info.title));
            if (success) {
                m.editMessage(
                        "Successfully added intro **" + info.title + "**" + (seek > 0 ? " with seek " + seek : ""))
                        .queue();
            } else {
                m.editMessage("Could not add intro...").queue();
            }
        }

        @Override
        public void noMatches() {
            event.replyError("No video found with this link");
            return;
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON)
                m.editMessage(event.getClient().getError() + " Error loading: " + throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError() + " Error loading track.").queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
        }
    }
}
