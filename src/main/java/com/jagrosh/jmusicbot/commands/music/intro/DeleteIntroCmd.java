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

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.OrderedMenu;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.IntroConfig;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;

/**
 *
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class DeleteIntroCmd extends MusicCommand {
    private OrderedMenu.Builder builder;

    public DeleteIntroCmd(Bot bot) {
        super(bot);
        this.name = "deleteintro";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.help = "let's you choose which of your intros to delete";
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
                            .setText("Choose which intro to delete by clicking one of the numbers below!")
                            .setChoices(new String[0])
                            .setSelection((msg, i) -> {
                                IntroConfig selectedIntro = introLinks[i - 1];
                                String confirmMessage = "Are you sure you want to delete intro **" + selectedIntro.getTitle() + "**? Click **1** to confirm, **X** to cancel.";
                                event.reply(confirmMessage, mes -> {
                                    builder
                                        .setColor(Color.RED)
                                        .setText(confirmMessage)
                                        .setChoices(new String[]{"Delete intro"})
                                        .setSelection((msg2, j) -> {
                                            boolean success = bot.getConfig().deleteIntro(userId, selectedIntro);
                                            String message = success
                                                ? "Successfully deleted intro **" + selectedIntro.getTitle() + "**" + (selectedIntro.getSeek() > 0 ? " with seek " + selectedIntro.getSeek() : "")
                                                : "Could not delete intro...";
                                            event.reply(message);
                                        })
                                        .setCancel((msg2) -> {})
                                        .setUsers(event.getAuthor());
                                    builder.build().display(mes);
                                });
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
}
