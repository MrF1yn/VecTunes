
package dev.mrflyn.vectunes;


import com.github.topi314.lavasrc.spotify.SpotifyAudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import dev.mrflyn.vectunes.searchmanagers.YouTubeSearchManager;
import io.sfrei.tracksearch.tracks.YouTubeTrack;

import java.util.*;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class VecTunesTrackManager extends AudioEventAdapter {
    private LinkedList<AudioTrack> trackQueue;
    private List<AudioTrack> persistentTrackQueue;
    private HashMap<String, Long> TRACK_REQUESTER = new HashMap<>();
    private List<String> requestedTracks = new ArrayList<String>();
    private AudioPlayer player;
    private long channelID;
    private long commandChannelID;
    private boolean queueLoop;
    private boolean songLoop;
    private boolean autoPlay;
    private boolean shuffle;
    private long guildID;
    private GUIManager guiManager;
    Random random = new Random();

    public VecTunesTrackManager(AudioPlayer player, long channelID, long guildID, long commandChannelID) {
        this.channelID = channelID;
        this.commandChannelID = commandChannelID;
        this.trackQueue = new LinkedList<AudioTrack>();
        this.persistentTrackQueue = new ArrayList<AudioTrack>();
        this.player = player;
        this.guildID = guildID;
        this.guiManager = new GUIManager(this, commandChannelID, guildID);
        VecTunes.bot.CHANNEL_TO_TUNES.put(channelID, this);
    }

    public String getAudioChannelName() {
        return Bot.jda.getGuildById(this.guildID).getVoiceChannelById(this.channelID).getAsMention();
    }

    public HashMap<String, Long> getTRACK_REQUESTER() {
        return this.TRACK_REQUESTER;
    }

    public void destroy() {
        this.guiManager.destroy();
        this.player.destroy();
        Guild guild = Bot.jda.getGuildById(this.guildID);
        if (guild != null) {
            guild.getAudioManager().closeAudioConnection();
        }
        VecTunes.bot.CHANNEL_TO_TUNES.remove(this.channelID);
    }

    public AudioPlayer getPlayer() {
        return this.player;
    }

    public boolean isQueueLoop() {
        return this.queueLoop;
    }

    public boolean isSongLoop() {
        return this.songLoop;
    }

    public boolean isAutoPlay() {
        return this.autoPlay;
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public String getRemainingQueueString() {
        StringBuilder s = new StringBuilder(" \n");
        int i = 1;
        for (AudioTrack t : this.trackQueue) {
            s.append(i++).append(": ").append(t.getInfo().title).append("\n");
        }
        return s.toString();
    }

    public void toggleQueueLoop() {
        this.queueLoop = !this.queueLoop;
        this.guiManager.update();
    }

    public void togglePlayPause() {
        this.player.setPaused(!this.player.isPaused());
        this.guiManager.update();
    }

    public void toggleSongLoop() {
        this.songLoop = !this.songLoop;
        this.guiManager.update();
    }

    public void toggleAutoPlay() {
        this.autoPlay = !this.autoPlay;
        this.guiManager.update();
    }

    public void toggleShuffle(){
        this.shuffle = !this.shuffle;
        this.guiManager.update();
    }

    public void skip() {
        this.player.stopTrack();
        AudioTrack audio = null;
        if (shuffle){
            audio = getRandom();
        }else {
            audio = this.trackQueue.poll();
        }
        if (audio != null) {
            this.player.playTrack(audio);
        }
        this.guiManager.update();
    }

    public void skip(int trackNo){
        if (trackNo<0||this.trackQueue.size()<=trackNo)return;
        this.player.stopTrack();
        this.player.playTrack(this.trackQueue.get(trackNo));
        this.trackQueue.remove(trackNo);
    }
    public int getRemainingTrackNumber(){
        return trackQueue.size();
    }
    public void forward(int secs) {
        if (secs <= 0) {
            return;
        }
        if (this.player.getPlayingTrack() == null) {
            return;
        }
        long pos = this.player.getPlayingTrack().getPosition() + (long)secs * 1000L;
        if (pos > this.player.getPlayingTrack().getDuration()) {
            return;
        }
        this.player.getPlayingTrack().setPosition(pos);
        this.guiManager.update();
    }

    public void rewind(int secs) {
        if (secs <= 0) {
            return;
        }
        if (this.player.getPlayingTrack() == null) {
            return;
        }
        long pos = this.player.getPlayingTrack().getPosition() - (long)secs * 1000L;
        if (pos < 0L) {
            return;
        }
        this.player.getPlayingTrack().setPosition(pos);
        this.guiManager.update();
    }

    public void queue(final String song, final Long requester, TextChannel channel, boolean force) {
        if (this.requestedTracks.contains(song)) {
            return;
        }
        this.requestedTracks.add(song);
        VecTunes.playerManager.loadItem(song, new AudioLoadResultHandler(){

            @Override
            public void trackLoaded(AudioTrack track) {
                VecTunesTrackManager.this.TRACK_REQUESTER.put(track.getIdentifier(), requester);
                if (force){
                    VecTunesTrackManager.this.player.stopTrack();
                    VecTunesTrackManager.this.player.playTrack(track);
                    return;
                }
                VecTunes.log(track.getIdentifier() + " added to queue!");
                if (channel!=null){
                    channel.sendMessage(track.getInfo().title +" added to Queue!").queue();
                }
                VecTunesTrackManager.this.trackQueue.add(track);
                VecTunesTrackManager.this.persistentTrackQueue.add(track.makeClone());


                if (VecTunesTrackManager.this.player.getPlayingTrack() == null && !VecTunesTrackManager.this.trackQueue.isEmpty()) {
                    VecTunesTrackManager.this.player.playTrack(VecTunesTrackManager.this.trackQueue.poll());
                }
//                VecTunesTrackManager.this.guiManager.update();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                VecTunes.log(playlist.getName() + " playlist added to queue!");
                if (channel!=null){
                    channel.sendMessage(playlist.getName() +" playlist added to Queue!").queue();
                }
                if (force){
                    VecTunesTrackManager.this.player.stopTrack();
                    VecTunesTrackManager.this.trackQueue.clear();
                    VecTunesTrackManager.this.persistentTrackQueue.clear();
                    VecTunesTrackManager.this.TRACK_REQUESTER.clear();
                }
                playlist.getTracks().forEach(t -> {
                    VecTunes.log("Added: " + t.getInfo().title);
                    VecTunesTrackManager.this.trackQueue.add(t.makeClone());
                    VecTunesTrackManager.this.persistentTrackQueue.add(t.makeClone());
                    VecTunesTrackManager.this.TRACK_REQUESTER.put(t.getIdentifier(), requester);
                });
                if (VecTunesTrackManager.this.player.getPlayingTrack() == null && !VecTunesTrackManager.this.trackQueue.isEmpty()) {
                    VecTunes.log("STARTED");
                    VecTunesTrackManager.this.player.playTrack(VecTunesTrackManager.this.trackQueue.poll());
                }
            }

            @Override
            public void noMatches() {
                VecTunesTrackManager.this.requestedTracks.remove(song);
                VecTunesTrackManager.this.guiManager.error("NOT FOUND");
                if (channel!=null){
                    channel.sendMessage(song+" not found!").queue();
                }
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                throwable.printStackTrace();
                VecTunesTrackManager.this.requestedTracks.remove(song);
                VecTunesTrackManager.this.guiManager.error("FAILED");
                channel.sendMessage(song+" failed to load!").queue();
            }
        });
        VecTunes.log("STARTING" + (this.player.getPlayingTrack() == null));
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        this.guiManager.update();
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        this.guiManager.update();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        VecTunes.log("STARTED");
        this.guiManager.update();
    }

    private AudioTrack getRandom(){
        int index = random.nextInt(trackQueue.size());
        AudioTrack audio  = trackQueue.get(index);
        trackQueue.remove(index);
        return audio;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        this.guiManager.sendSongEndEmbed(track);
        this.requestedTracks.remove(track.getInfo().uri);
        this.guiManager.update();
        if (endReason == AudioTrackEndReason.STOPPED) {
            return;
        }
        if (this.songLoop) {
            player.playTrack(track.makeClone());
            VecTunes.log("Looping Song!");
            return;
        }
        AudioTrack audio = null;

        if (!trackQueue.isEmpty()){
            if (shuffle){
                audio = getRandom();
            }else {
                audio = trackQueue.poll();
            }
        }

        if (audio == null) {
            VecTunes.log("Queue finished!");
            if (!this.queueLoop && !this.autoPlay) {
                this.destroy();
                return;
            }
            if (this.queueLoop) {
                this.trackQueue = new LinkedList<AudioTrack>();
                this.persistentTrackQueue.forEach(t -> this.trackQueue.add(t.makeClone()));
                audio = this.trackQueue.poll();
                VecTunes.log("Looping queue!");
            } else if (this.autoPlay) {
                if (VecTunes.spotifySearchManager != null && YouTubeSearchManager.GUILD_SPOTIFY_CREDENTIALS.containsKey(guildID)) {
                    if (track instanceof SpotifyAudioTrack) {
                        SpotifyAudioTrack spotifyAudioTrack = (SpotifyAudioTrack)track;
                        for (SpotifyAudioTrack tracks : VecTunes.spotifySearchManager.getAutoPlayList(spotifyAudioTrack, guildID)) {
                            this.queue(guildID+" "+tracks.getInfo().uri, Bot.jda.getSelfUser().getIdLong(), null, false);
                        }
                        return;
                    }
                    for (SpotifyAudioTrack tracks : VecTunes.spotifySearchManager.getAutoPlayListFromName(track.getInfo().title + " " + track.getInfo().author, guildID)) {
                        this.queue(guildID+" "+tracks.getInfo().uri, Bot.jda.getSelfUser().getIdLong(), null, false);
                    }
                    return;
                }
                if (!(track instanceof YoutubeAudioTrack)) {
                    for (YouTubeTrack ytTrack : VecTunes.youTubeSearchManager.getAutoPlayListFromName(track.getInfo().title + " " + track.getInfo().author)) {
                        this.queue(ytTrack.getUrl(), Bot.jda.getSelfUser().getIdLong(), null, false);
                    }
                    return;
                }
                for (YouTubeTrack ytTrack : VecTunes.youTubeSearchManager.getAutoPlayList(track.getInfo().identifier)) {
                    this.queue(ytTrack.getUrl(), Bot.jda.getSelfUser().getIdLong(), null, false);
                }
                return;
            }
        }
            player.playTrack(audio);
            VecTunes.log("playing " + audio.getIdentifier());
            return;
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        this.requestedTracks.remove(track.getInfo().uri);
        this.guiManager.error("EXCEPTION");
        exception.printStackTrace();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        this.requestedTracks.remove(track.getInfo().uri);
        this.skip();
        this.guiManager.error("STUCK");
        VecTunes.log("STUCK");

    }
}

