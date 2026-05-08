package spz.meowing.media;

/**
 * Immutable snapshot of current media playback state.
 */
public class MediaState {

    public static final MediaState EMPTY = new MediaState("", "", false, false, 0, 0, "");

    private final String title;
    private final String artist;
    private final boolean active;
    private final boolean playing;
    private final long positionMs;
    private final long durationMs;
    private final String trackId;

    public MediaState(String title, String artist, boolean active, boolean playing,
                      long positionMs, long durationMs, String trackId) {
        this.title = title;
        this.artist = artist;
        this.active = active;
        this.playing = playing;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.trackId = trackId;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public boolean isActive() { return active; }
    public boolean isPlaying() { return playing; }
    public long getPositionMs() { return positionMs; }
    public long getDurationMs() { return durationMs; }
    public String getTrackId() { return trackId; }

    public String getPositionFormatted() {
        long secs = positionMs / 1000;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    public String getDurationFormatted() {
        if (durationMs <= 0) return "";
        long secs = durationMs / 1000;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    public float getProgress() {
        if (durationMs <= 0 || positionMs <= 0) return 0f;
        return Math.min(1f, (float) positionMs / durationMs);
    }

    public boolean hasTrackInfo() {
        return title != null && !title.isEmpty();
    }
}
