package dev.kuroinu.nova.platform;

import java.util.List;

/**
 * The saved multiplayer server list plus live status pings, backing Nova's Multiplayer screen.
 * All methods are called on the render thread; pinging happens in the background.
 */
public interface ServerBrowser {
    enum Status {PINGING, ONLINE, UNREACHABLE, OUTDATED}

    /** Snapshot of one saved server. Text fields may contain § formatting codes. */
    final class Server {
        public final String name;
        public final String address;
        public final String motd;
        public final String players;
        public final String version;
        public final long ping;
        public final Status status;

        public Server(String name, String address, String motd, String players, String version, long ping, Status status) {
            this.name = name;
            this.address = address;
            this.motd = motd;
            this.players = players;
            this.version = version;
            this.ping = ping;
            this.status = status;
        }
    }

    /** Loads the saved list from disk and starts pinging every server. */
    void open();

    /** Stops pinging (when the screen is hidden). */
    void close();

    /** Drives the pinger; call every frame while the screen is visible. */
    void tick();

    List<Server> servers();

    /** Re-pings every server. */
    void refresh();

    void join(int index);

    void directConnect(String address);

    void add(String name, String address);

    void edit(int index, String name, String address);

    void remove(int index);

    /** Swaps two entries (for move up/down). */
    void swap(int a, int b);

    /** Draws the server's favicon; returns false when it has none (the screen draws a placeholder). */
    boolean drawIcon(int index, float x, float y, float size);
}
