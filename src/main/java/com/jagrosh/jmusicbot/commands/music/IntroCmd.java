package com.jagrosh.jmusicbot.commands.music;

import java.util.Random;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.MusicCommand;

import net.dv8tion.jda.api.entities.User;


/**
 * 
 * @author Kevin Greiner <kevin.greiner1999@gmail.com>
 */
public class IntroCmd extends MusicCommand
{
    private final static String LOAD = "\uD83D\uDCE5"; // 📥
    private final static String CANCEL = "\uD83D\uDEAB"; // 🚫
    
    private final String loadingEmoji;

    public IntroCmd(Bot bot) {
        super(bot);
        this.loadingEmoji = bot.getConfig().getLoading();
        this.name = "intro";
        this.help = "plays one of the user`s intros";
        this.aliases = bot.getConfig().getAliases(this.name);
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) 
    {
        Long userId = event.getMember().getIdLong();
        String[] introLinks = bot.getConfig().getIntros(userId);
        if (introLinks.length == 0) {
            event.replyWarning("There are no intros set for this user.");

            // send private message with user`s id to me
            User owner = bot.getJDA().retrieveUserById(bot.getConfig().getOwnerId()).complete();
            owner.openPrivateChannel().queue(pc -> pc.sendMessage("Not intros set for user <"+ event.getMember().getUser().getName()+"> with Id <"+userId+">").queue());
            return;
        }

        String selectedIntro = introLinks[new Random().nextInt(introLinks.length)];
    }
}

