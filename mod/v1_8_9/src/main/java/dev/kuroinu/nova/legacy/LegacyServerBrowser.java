package dev.kuroinu.nova.legacy;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.kuroinu.nova.platform.ServerBrowser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.8.9 server list for Nova's Multiplayer screen, mirroring vanilla MultiplayerScreen: the saved
 * {@link ServerList}, a {@link MultiplayerServerListPinger} driven from the render thread, DNS +
 * connect on a small background pool, and favicons uploaded as dynamic textures.
 */
public final class LegacyServerBrowser implements ServerBrowser {
    private static final int PROTOCOL_1_8 = 47;
    private static final AtomicInteger THREADS = new AtomicInteger();

    private final ExecutorService pool = Executors.newFixedThreadPool(5, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Nova Server Pinger #" + THREADS.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });
    private ServerList list;
    private MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    /** Uploaded favicons keyed by ServerInfo identity: {base64, texture id}. */
    private final Map<ServerInfo, Object[]> icons = new HashMap<ServerInfo, Object[]>();

    private static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    @Override
    public void open() {
        list = new ServerList(mc());
        list.loadFile();
        refresh();
    }

    @Override
    public void close() {
        pinger.cancel();
        for (Object[] icon : icons.values()) {
            mc().getTextureManager().close((Identifier) icon[1]);
        }
        icons.clear();
    }

    @Override
    public void tick() {
        pinger.tick();
    }

    @Override
    public void refresh() {
        pinger.cancel();
        pinger = new MultiplayerServerListPinger();
        if (list == null) return;
        for (int i = 0; i < list.size(); i++) {
            ping(list.get(i));
        }
    }

    private void ping(final ServerInfo info) {
        info.online = true;
        info.ping = -2L;
        info.label = "";
        info.playerCountLabel = "";
        final MultiplayerServerListPinger current = pinger;
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    current.add(info);
                } catch (UnknownHostException e) {
                    info.ping = -1L;
                    info.label = "§4Can't resolve hostname";
                } catch (Exception e) {
                    info.ping = -1L;
                    info.label = "§4Can't connect to server";
                }
            }
        });
    }

    @Override
    public List<Server> servers() {
        List<Server> out = new ArrayList<Server>();
        if (list == null) return out;
        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            Status status;
            if (info.ping == -2L) {
                status = Status.PINGING;
            } else if (info.ping < 0) {
                status = Status.UNREACHABLE;
            } else if (info.protocolVersion != PROTOCOL_1_8) {
                status = Status.OUTDATED;
            } else {
                status = Status.ONLINE;
            }
            out.add(new Server(info.name, info.address, info.label, info.playerCountLabel, info.version, info.ping, status));
        }
        return out;
    }

    @Override
    public void join(int index) {
        if (list == null || index < 0 || index >= list.size()) return;
        mc().setScreen(new ConnectScreen(mc().currentScreen, mc(), list.get(index)));
    }

    @Override
    public void directConnect(String address) {
        ServerInfo info = new ServerInfo("Direct Connect", address, false);
        mc().setScreen(new ConnectScreen(mc().currentScreen, mc(), info));
    }

    @Override
    public void add(String name, String address) {
        ServerInfo info = new ServerInfo(name, address, false);
        list.add(info);
        list.saveFile();
        ping(info);
    }

    @Override
    public void edit(int index, String name, String address) {
        ServerInfo info = list.get(index);
        info.name = name;
        info.address = address;
        list.saveFile();
        ping(info);
    }

    @Override
    public void remove(int index) {
        Object[] icon = icons.remove(list.get(index));
        if (icon != null) mc().getTextureManager().close((Identifier) icon[1]);
        list.remove(index);
        list.saveFile();
    }

    @Override
    public void swap(int a, int b) {
        list.swapEntries(a, b);
        list.saveFile();
    }

    @Override
    public boolean drawIcon(int index, float x, float y, float size) {
        if (list == null || index < 0 || index >= list.size()) return false;
        Identifier id = icon(list.get(index));
        if (id == null) return false;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(size / 64f, size / 64f, 1f);
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc().getTextureManager().bindTexture(id);
        DrawableHelper.drawTexture(0, 0, 0f, 0f, 64, 64, 64f, 64f);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        return true;
    }

    /** Uploads (or reuses) the favicon texture; null when the server sent none or it's invalid. */
    private Identifier icon(ServerInfo info) {
        String b64 = info.getIcon();
        Object[] cached = icons.get(info);
        if (b64 == null) {
            return null;
        }
        if (cached != null && b64.equals(cached[0])) {
            return (Identifier) cached[1];
        }
        try {
            byte[] png = Base64.getDecoder().decode(b64.replace("\n", ""));
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
            if (img == null || img.getWidth() != 64 || img.getHeight() != 64) {
                return null;
            }
            Identifier id = cached != null ? (Identifier) cached[1]
                    : new Identifier("nova", "servers/" + Integer.toHexString(System.identityHashCode(info)) + "/icon");
            mc().getTextureManager().loadTexture(id, new NativeImageBackedTexture(img));
            icons.put(info, new Object[]{b64, id});
            return id;
        } catch (Exception e) {
            info.setIcon(null); // don't retry a broken icon every frame
            return null;
        }
    }
}
