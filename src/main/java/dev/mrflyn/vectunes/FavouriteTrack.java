
package dev.mrflyn.vectunes;

import java.io.Serializable;
import java.util.Objects;

public class FavouriteTrack
implements Serializable {
    private final long userID;
    private final String trackName;
    private final String thumbnailURL;
    private final String trackURL;

    public FavouriteTrack(long userID, String trackName, String thumbnailURL, String trackURL) {
        this.userID = userID;
        this.trackName = trackName;
        this.thumbnailURL = thumbnailURL;
        this.trackURL = trackURL;
    }

    public long getUserID() {
        return this.userID;
    }

    public String getTrackName() {
        return this.trackName;
    }

    public String getThumbnailURL() {
        return this.thumbnailURL;
    }

    public String getTrackURL() {
        return this.trackURL;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        FavouriteTrack that = (FavouriteTrack)o;
        return this.userID == that.userID && this.trackName.equals(that.trackName) && this.thumbnailURL.equals(that.thumbnailURL) && this.trackURL.equals(that.trackURL);
    }

    public int hashCode() {
        return Objects.hash(this.userID, this.trackName, this.thumbnailURL, this.trackURL);
    }
}

