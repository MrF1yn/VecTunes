
package dev.mrflyn.vectunes.searchmanagers;

import com.github.topisenpai.lavasrc.spotify.SpotifyAudioTrack;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import okhttp3.OkHttpClient;

public class SpotifySearchManager {
    private String clientID;
    private String clientSecret;
    private String countryCode;
    private OkHttpClient client;
    private SpotifySourceManager spotifySourceManager;

    public SpotifySearchManager(String clientID, String clientSecret, String countryCode, SpotifySourceManager spotifySourceManager) {
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.countryCode = countryCode;
        this.client = new OkHttpClient();
        this.spotifySourceManager = spotifySourceManager;
    }

    public BasicAudioPlaylist getPlaylistFromSearch(String query) {
        try {
            AudioItem item = this.spotifySourceManager.getSearch(query);
            if (item == AudioReference.NO_TRACK) {
                return null;
            }
            if (!(item instanceof BasicAudioPlaylist)) {
                return null;
            }
            return (BasicAudioPlaylist)item;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<SpotifyAudioTrack> getAutoPlayList(SpotifyAudioTrack track) {
        try {
            AudioItem item = this.spotifySourceManager.getRecommendations("seed_artists=" + track.getArtistID() + "&seed_genres=any&seed_tracks=" + track.getInfo().identifier);
            if (item == AudioReference.NO_TRACK) {
                return new ArrayList<SpotifyAudioTrack>();
            }
            if (!(item instanceof BasicAudioPlaylist)) {
                return new ArrayList<SpotifyAudioTrack>();
            }
            return ((BasicAudioPlaylist)item).getTracks().stream().map(t -> (SpotifyAudioTrack)t).collect(Collectors.toList());
        }
        catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<SpotifyAudioTrack>();
        }
    }

    public List<SpotifyAudioTrack> getAutoPlayListFromName(String name) {
        BasicAudioPlaylist list = this.getPlaylistFromSearch(name);
        if (list == null) {
            return new ArrayList<SpotifyAudioTrack>();
        }
        return this.getAutoPlayList((SpotifyAudioTrack)list.getTracks().get(0));
    }
}

