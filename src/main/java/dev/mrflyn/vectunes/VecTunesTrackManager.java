
package dev.mrflyn.vectunes;

import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import dev.mrflyn.vectunes.Bot;
import dev.mrflyn.vectunes.GUIManager;
import dev.mrflyn.vectunes.VecTunes;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.dv8tion.jda.api.entities.Guild;

public class VecTunesTrackManager
extends AudioEventAdapter {
    private Queue<AudioTrack> trackQueue;
    private List<AudioTrack> persistentTrackQueue;
    private HashMap<String, Long> TRACK_REQUESTER = new HashMap<>();
    private List<String> requestedTracks = new ArrayList<String>();
    private AudioPlayer player;
    private long channelID;
    private long commandChannelID;
    private boolean queueLoop;
    private boolean songLoop;
    private boolean autoPlay;
    private long guildID;
    private GUIManager guiManager;

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

    public void skip() {
        this.player.stopTrack();
        AudioTrack audio = this.trackQueue.poll();
        if (audio != null) {
            this.player.playTrack(audio);
        }
        this.guiManager.update();
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

    public void queue(final String song, final Long requester) {
        if (this.requestedTracks.contains(song)) {
            return;
        }
        this.requestedTracks.add(song);
        VecTunes.playerManager.loadItem(song, new AudioLoadResultHandler(){

            @Override
            public void trackLoaded(AudioTrack track) {
                VecTunes.log(track.getIdentifier() + " added to queue!");
                VecTunesTrackManager.this.trackQueue.add(track);
                VecTunesTrackManager.this.persistentTrackQueue.add(track.makeClone());
                VecTunesTrackManager.this.TRACK_REQUESTER.put(track.getIdentifier(), requester);
                if (VecTunesTrackManager.this.player.getPlayingTrack() == null && !VecTunesTrackManager.this.trackQueue.isEmpty()) {
                    VecTunesTrackManager.this.player.playTrack(VecTunesTrackManager.this.trackQueue.poll());
                }
                VecTunesTrackManager.this.guiManager.update();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                VecTunes.log(playlist.getName() + " playlist added to queue!");
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
                VecTunesTrackManager.this.guiManager.update();
            }

            @Override
            public void noMatches() {
                VecTunesTrackManager.this.requestedTracks.remove(song);
                VecTunesTrackManager.this.guiManager.error("NOT FOUND");
            }

            @Override
            public void loadFailed(FriendlyException throwable) {
                throwable.printStackTrace();
                VecTunesTrackManager.this.requestedTracks.remove(song);
                VecTunesTrackManager.this.guiManager.error("FAILED");
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

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        this.guiManager.sendSongEndEmbed(track);
        this.requestedTracks.remove(track.getInfo().uri);
        this.guiManager.update();
        if (endReason == AudioTrackEndReason.STOPPED || endReason == AudioTrackEndReason.REPLACED) {
            return;
        }
        if (this.songLoop) {
            player.playTrack(track.makeClone());
            VecTunes.log("Looping Song!");
            return;
        }
        AudioTrack audio = this.trackQueue.poll();
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
                if (VecTunes.spotifySearchManager != null) {
                    if (track instanceof SpotifyAudioTrack) {
                        SpotifyAudioTrack spotifyAudioTrack = (SpotifyAudioTrack)track;
                        for (SpotifyAudioTrack tracks : VecTunes.spotifySearchManager.getAutoPlayList(spotifyAudioTrack)) {
                            this.queue(tracks.getInfo().uri, Bot.jda.getSelfUser().getIdLong());
                        }
                        return;
                    }
                    for (SpotifyAudioTrack tracks : VecTunes.spotifySearchManager.getAutoPlayListFromName(track.getInfo().title + " " + track.getInfo().author)) {
                        this.queue(tracks.getInfo().uri, Bot.jda.getSelfUser().getIdLong());
                    }
                    return;
                }
                if (!(track instanceof YoutubeAudioTrack)) {
                    for (YouTubeTrack ytTrack : VecTunes.youTubeSearchManager.getAutoPlayListFromName(track.getInfo().title + " " + track.getInfo().author)) {
                        this.queue(ytTrack.getUrl(), Bot.jda.getSelfUser().getIdLong());
                    }
                    return;
                }
                for (YouTubeTrack ytTrack : VecTunes.youTubeSearchManager.getAutoPlayList(track.getInfo().identifier)) {
                    this.queue(ytTrack.getUrl(), Bot.jda.getSelfUser().getIdLong());
                }
                return;
            }
        }
        if (endReason.mayStartNext) {
            player.playTrack(audio);
            VecTunes.log("playing " + audio.getIdentifier());
            return;
        }
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

