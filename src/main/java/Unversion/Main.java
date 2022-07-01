package Unversion;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;

public class Main extends JavaPlugin {
    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        Bukkit.getLogger().info(String.format("[%s] - Enabled", getDescription().getName()));
        if (protocolManager != null) {
            protocolManager.addPacketListener(
                    new PacketAdapter(this, ListenerPriority.NORMAL,
                            PacketType.Status.Server.SERVER_INFO) {
                        @Override
                        public void onPacketSending(PacketEvent event) {
                            if (event.getPacketType() ==
                                    PacketType.Status.Server.SERVER_INFO) {

                                WrappedServerPing packet = event.getPacket().getServerPings().read(0);
                                packet.setVersionName(ChatColor.translateAlternateColorCodes('&', config.getString("version.serverList", "Unknown")));
                            }
                        }
                    });

            protocolManager.addPacketListener(
                    new PacketAdapter(this, ListenerPriority.NORMAL,
                            PacketType.Play.Server.ENTITY_STATUS) {
                        @Override
                        public void onPacketSending(PacketEvent event) {
                            if (event.getPacketType() != PacketType.Play.Server.ENTITY_STATUS) {
                                return;
                            }

                            ByteBuf buf = Unpooled.buffer();
                            writeString(ChatColor.translateAlternateColorCodes('&', config.getString("version.f3", "Unknown")), buf);
                            WrapperPlayServerCustomPayload wrapperPlayServerCustomPayload = new WrapperPlayServerCustomPayload();
                            wrapperPlayServerCustomPayload.setChannel(new MinecraftKey("brand"));
                            wrapperPlayServerCustomPayload.setContentsBuffer(buf);
                            wrapperPlayServerCustomPayload.sendPacket(event.getPlayer());
                        }
                    });
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getLogger().severe(String.format("[%s] - Disabled", getDescription().getName()));
    }

    private void writeString(String s, ByteBuf buf) {
        if (s.length() > Short.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("Cannot send string longer than Short.MAX_VALUE (got %s characters)", s.length()));
        }

        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(b.length, buf);
        buf.writeBytes(b);

    }

    private void writeVarInt(int value, ByteBuf output) {
        int part;
        while (true) {
            part = value & 0x7F;

            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }

            output.writeByte(part);

            if (value == 0) {
                break;
            }
        }
    }
}
