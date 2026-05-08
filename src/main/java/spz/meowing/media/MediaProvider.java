package spz.meowing.media;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Media state provider using Spotify window title detection.
 *
 * Elapsed time is tracked purely in Java by measuring how long
 * the current song has been playing:
 * - When a new song is detected (title changes): reset timer
 * - When paused (title = "Spotify"/"Spotify Premium"): freeze timer
 * - When resumed (title goes back to "Artist - Song"): continue timer
 *
 * Simple, reliable, no GSMTC/assembly loading overhead.
 */
public final class MediaProvider {

    private static final AtomicReference<MediaState> currentState = new AtomicReference<>(MediaState.EMPTY);
    private static volatile boolean running = false;

    // Elapsed time tracking
    private static String currentTrackId = "";
    private static long accumulatedMs = 0;      // time accumulated before current play session
    private static long lastResumeTime = 0;      // when we last started/resumed playing
    private static boolean wasPlaying = false;

    private MediaProvider() {}

    public static MediaState getState() {
        MediaState polled = currentState.get();
        if (!polled.isActive() || !polled.hasTrackInfo()) return polled;

        // Calculate real-time elapsed
        long elapsed;
        if (wasPlaying && lastResumeTime > 0) {
            elapsed = accumulatedMs + (System.currentTimeMillis() - lastResumeTime);
        } else {
            elapsed = accumulatedMs;
        }

        return new MediaState(
                polled.getTitle(), polled.getArtist(), polled.isActive(),
                polled.isPlaying(), elapsed, polled.getDurationMs(), polled.getTrackId()
        );
    }

    public static void start() {
        if (running) return;
        running = true;
        Thread.ofVirtual().start(MediaProvider::pollLoop);
    }

    public static void stop() {
        running = false;
        currentState.set(MediaState.EMPTY);
        currentTrackId = "";
        accumulatedMs = 0;
        lastResumeTime = 0;
        wasPlaying = false;
    }

    private static void pollLoop() {
        while (running) {
            try {
                poll();
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                try { Thread.sleep(1000); } catch (InterruptedException ex) { break; }
            }
        }
    }

    private static void poll() {
        try {
            // Simple, fast PowerShell command - just get window title
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command",
                    "(Get-Process Spotify -EA 0|?{$_.MainWindowTitle -ne ''}).MainWindowTitle");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            String title = null;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) title = line.trim();
                }
            }
            if (!proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                return;
            }

            if (title == null || title.isBlank()) {
                // Spotify not running
                if (wasPlaying) {
                    accumulatedMs += System.currentTimeMillis() - lastResumeTime;
                    wasPlaying = false;
                }
                currentState.set(MediaState.EMPTY);
                return;
            }

            if (title.contains(" - ")) {
                // Playing: "Artist - Song"
                String[] parts = title.split(" - ", 2);
                String artist = parts[0].trim();
                String track = parts[1].trim();
                String trackId = artist + "|" + track;

                // Track changed — reset timer
                if (!trackId.equals(currentTrackId)) {
                    currentTrackId = trackId;
                    accumulatedMs = 0;
                    lastResumeTime = System.currentTimeMillis();
                    wasPlaying = true;
                }

                // Was paused, now resumed
                if (!wasPlaying) {
                    lastResumeTime = System.currentTimeMillis();
                    wasPlaying = true;
                }

                currentState.set(new MediaState(track, artist, true, true, 0, 0, trackId));

            } else if (title.equalsIgnoreCase("Spotify") || title.equalsIgnoreCase("Spotify Premium")
                    || title.equalsIgnoreCase("Spotify Free")) {
                // Paused — freeze timer
                if (wasPlaying && lastResumeTime > 0) {
                    accumulatedMs += System.currentTimeMillis() - lastResumeTime;
                    wasPlaying = false;
                }

                MediaState prev = currentState.get();
                if (prev.hasTrackInfo()) {
                    currentState.set(new MediaState(prev.getTitle(), prev.getArtist(), true, false,
                            accumulatedMs, 0, prev.getTrackId()));
                }
            }

        } catch (Exception ignored) {}
    }
}
