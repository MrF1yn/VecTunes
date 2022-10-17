
package dev.mrflyn.vectunes.searchmanagers;

import dev.mrflyn.vectunes.FavouriteTrack;
import dev.mrflyn.vectunes.StringUtil;
import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.clients.youtube.YouTubeClient;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class YouTubeSearchManager {
    private static HashMap<String, String> CACHED_SEARCHES = new HashMap<>();
    public static HashMap<Long, List<FavouriteTrack>> FAVOURITES = new HashMap<>();
    private TrackSearchClient<YouTubeTrack> explicitClient = new YouTubeClient();

    public static void saveAutoCompletes() {
        try {
            File file = new File("AutoCompletes.dat");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream("AutoCompletes.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(CACHED_SEARCHES);
            oos.close();
            fos.close();
            System.out.println("Saved AutoCompletes!");
            file = new File("Favourites.dat");
            if (!file.exists()) {
                file.createNewFile();
            }
            fos = new FileOutputStream("Favourites.dat");
            oos = new ObjectOutputStream(fos);
            oos.writeObject(FAVOURITES);
            oos.close();
            fos.close();
            System.out.println("Saved Favourites!");
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public static void readAutoCompletes() {
        try {
            File file = new File("AutoCompletes.dat");
            if (!file.exists()) {
                file.createNewFile();
                YouTubeSearchManager.saveAutoCompletes();
            }
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            CACHED_SEARCHES = (HashMap)ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Loaded AutoCompletes!");
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public static void readFavourites() {
        try {
            File file = new File("Favourites.dat");
            if (!file.exists()) {
                file.createNewFile();
                YouTubeSearchManager.saveAutoCompletes();
            }
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            FAVOURITES = (HashMap)ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Loaded Favourites!");
        }
        catch (Exception ioe) {
            ioe.printStackTrace();
        }
    }

    public void validateAutoCompletes() {
    }

    public String getLinkFromQuery(String query) {
        try {
            String link = CACHED_SEARCHES.get(query);
            if (link == null) {
                TrackList<YouTubeTrack> tracksForSearch = this.explicitClient.getTracksForSearch(query);
                if (tracksForSearch.isEmpty()) {
                    return null;
                }
                link = tracksForSearch.getTracks().get(0).getUrl();
            }
            return link;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<YouTubeTrack> getAutoPlayList(String videoID) {
        try {
            TrackList<YouTubeTrack> tracksForSearch = this.explicitClient.getRelatedTracks(videoID);
            if (tracksForSearch.isEmpty()) {
                return new ArrayList<YouTubeTrack>();
            }
            return tracksForSearch.getTracks();
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<YouTubeTrack>();
        }
    }

    public List<YouTubeTrack> getAutoPlayListFromName(String name) {
        try {
            TrackList<YouTubeTrack> tracksForSearch = this.explicitClient.getTracksForSearch(name);
            if (tracksForSearch.isEmpty()) {
                return new ArrayList<YouTubeTrack>();
            }
            String[] rawID = tracksForSearch.getTracks().get(0).getUrl().split("v=");
            if (rawID.length < 2) {
                return new ArrayList<YouTubeTrack>();
            }
            TrackList<YouTubeTrack> relatedTracks = this.explicitClient.getRelatedTracks(rawID[1]);
            if (relatedTracks.isEmpty()) {
                return new ArrayList<YouTubeTrack>();
            }
            return relatedTracks.getTracks();
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<YouTubeTrack>();
        }
    }

    public List<String> getTitlesFromQuery(String query) {
        ArrayList<String> titles = new ArrayList<String>();
        try {
            TrackList<YouTubeTrack> tracksForSearch = this.explicitClient.getTracksForSearch(query);
            for (YouTubeTrack title : tracksForSearch.getTracks()) {
                titles.add(title.getTitle());
                CACHED_SEARCHES.put(title.getTitle(), title.getUrl());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return titles;
    }

    public List<String> getAutoCompletes(String query) {
        List<String> autoCompletes = YouTubeSearchManager.sortedResults(query, new ArrayList<String>(CACHED_SEARCHES.keySet()));
        if (autoCompletes.size() < 5) {
            autoCompletes = YouTubeSearchManager.sortedResults(query, this.getTitlesFromQuery(query));
        }
        if (autoCompletes.size() > 25) {
            autoCompletes = autoCompletes.subList(0, 25);
        }
        return autoCompletes;
    }

    public String spotifyTOYoutube(String url) {
        return null;
    }

    public static List<String> sortedResults(String arg, List<String> results) {
        ArrayList completions = new ArrayList<>();
        StringUtil.copyPartialMatches(arg, results, completions);
        Collections.sort(completions);
        results.clear();
        results.addAll(completions);
        return results;
    }
}

