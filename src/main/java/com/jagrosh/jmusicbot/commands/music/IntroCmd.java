package com.jagrosh.jmusicbot.commands.music;

import java.util.Random;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.MusicCommand;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;


/**
 * 
 * @author Kevin Greiner <kevin.greiner1999@gmail.com>
 */
public class IntroCmd extends MusicCommand
{
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
        event.reply(loadingEmoji+" Loading... `["+selectedIntro+"]`", m -> bot.getPlayerManager().loadItemOrdered(event.getGuild(), selectedIntro, new ResultHandler(m,event,false)));
    }

    private class ResultHandler implements AudioLoadResultHandler
    {
        private final Message m;
        private final CommandEvent event;
        private final boolean ytsearch;
        
        private ResultHandler(Message m, CommandEvent event, boolean ytsearch)
        {
            this.m = m;
            this.event = event;
            this.ytsearch = ytsearch;
        }
        
        private void loadSingle(AudioTrack track)
        {
            if(bot.getConfig().isTooLong(track))
            {
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" This track (**"+track.getInfo().title+"**) is longer than the allowed maximum: `"
                        +FormatUtil.formatTime(track.getDuration())+"` > `"+FormatUtil.formatTime(bot.getConfig().getMaxSeconds()*1000)+"`")).queue();
                return;
            }
            AudioHandler handler = (AudioHandler)event.getGuild().getAudioManager().getSendingHandler();
            int pos = handler.addTrack(new QueuedTrack(track, event.getAuthor()))+1;
            String addMsg = FormatUtil.filter(event.getClient().getSuccess()+" Added **"+track.getInfo().title
                    +"** (`"+FormatUtil.formatTime(track.getDuration())+"`) "+(pos==0?"to begin playing":" to the queue at position "+pos));
            m.editMessage(addMsg).queue();
        }

        @Override
        public void trackLoaded(AudioTrack track)
        {
            loadSingle(track);
        }

        @Override
        public void noMatches()
        {
            if(ytsearch)
                m.editMessage(FormatUtil.filter(event.getClient().getWarning()+" No results found for `"+event.getArgs()+"`.")).queue();
            else
                bot.getPlayerManager().loadItemOrdered(event.getGuild(), "ytsearch:"+event.getArgs(), new ResultHandler(m,event,true));
        }

        @Override
        public void loadFailed(FriendlyException throwable)
        {
            if(throwable.severity==Severity.COMMON)
                m.editMessage(event.getClient().getError()+" Error loading: "+throwable.getMessage()).queue();
            else
                m.editMessage(event.getClient().getError()+" Error loading track.").queue();
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist){}
    }
}

