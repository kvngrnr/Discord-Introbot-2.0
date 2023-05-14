/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.IntroConfig;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class ChooseIntroCmd extends MusicCommand {
    private final OrderedMenu.Builder builder;
    private final String loadingEmoji;

    public ChooseIntroCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "chooseintro";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.help = "let's you choose which of your intros to play";
        this.beListening = true;
        this.bePlaying = false;
        this.botPermissions = new Permission[] { Permission.MESSAGE_EMBED_LINKS };
        builder = new OrderedMenu.Builder()
                .allowTextInput(true)
                .useNumbers()
                .useCancelButton(true)
                .setEventWaiter(bot.getWaiter())
                .setTimeout(1, TimeUnit.MINUTES);
    }

    @Override
    public void doCommand(CommandEvent event) {
        Long userId = event.getMember().getIdLong();
        IntroConfig[] introLinks = bot.getConfig().getIntros(userId);

        if (introLinks.length == 0) {
            event.replyWarning("There are no intros set for this user.");

            // send private message with user`s id to me
            User owner = bot.getJDA().retrieveUserById(bot.getConfig().getOwnerId()).complete();
            owner.openPrivateChannel().queue(pc -> pc.sendMessage(
                    "No intros set for user <" + event.getMember().getUser().getName() + "> with Id <" + userId + ">")
                    .queue());
            return;
        }

        event.reply("Loading intros",
                m -> {
                    builder
                            .setColor(event.getSelfMember().getColor())
                            .setText("Choose an intro by clicking one of the numbers below!")
                            .setChoices(new String[0])
                            .setSelection((msg, i) -> {
                                IntroConfig selectedIntro = introLinks[i - 1];
                                event.reply(
                                        loadingEmoji + " Loading... `[" + selectedIntro.getLink() + "]`",
                                        mes -> bot.getPlayerManager().loadItemOrdered(
                                            event.getGuild(),
                                            selectedIntro.getLink(),
                                            new ResultHandler(mes, event, selectedIntro.getSeek())
                                        ));
                            })
                            .setCancel((msg) -> {
                            })
                            .setUsers(event.getAuthor());

                    for (int i = 0; i < introLinks.length; i++) {
                        builder.addChoices("**" + introLinks[i].getTitle() + "**");
                    }
                    builder.build().display(m);
                });
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
            if (bot.getConfig().isTooLong(track)) {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning() + " This track (**"
                        + track.getInfo().title + "**) is longer than the allowed maximum: `"
                        + FormatUtil.formatTime(track.getDuration()) + "` > `" + bot.getConfig().getMaxTime() + "`"))
                        .queue();
                return;
            }
            AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor(), seek)) + 1;
            m.editMessage(FormatUtil.filter(event.getClient().getSuccess() + " Added **" + track.getInfo().title
                    + "** (`" + FormatUtil.formatTime(track.getDuration()) + "`) " + (pos == 0 ? "to begin playing"
                            : " to the queue at position " + pos)))
                    .queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {}

        @Override
        public void noMatches() {
            m.editMessage(FormatUtil
                    .filter(event.getClient().getWarning() + " No results found for `" + event.getArgs() + "`."))
                    .queue();
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == Severity.COMMON)
                m.editMessage(event.getClient().getError() + " Error loading: " + throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError() + " Error loading track.").queue();
        }
    }
}
