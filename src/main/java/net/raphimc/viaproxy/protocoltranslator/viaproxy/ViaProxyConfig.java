/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.viaproxy.protocoltranslator.viaproxy;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.lenni0451.optconfig.ConfigContext;
import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.annotations.*;
import net.lenni0451.optconfig.annotations.cli.CLIAliases;
import net.lenni0451.optconfig.annotations.cli.CLIName;
import net.lenni0451.optconfig.migrate.ConfigMigrator;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.config.*;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

@OptConfig(header = "ViaProxy configuration file", version = 2)
@Migrator(from = 1, to = 2, migrator = ViaProxyConfig.Migrator1To2.class)
public class ViaProxyConfig {

    private ConfigContext<ViaProxyConfig> configContext;

    @Option("frontend")
    @Description("These options affect the behavior of the proxy related to client connections.")
    private Frontend frontend = new Frontend();

    @Option("proxy")
    @Description("These options affect the general behavior of the proxy.")
    private Proxy proxy = new Proxy();

    @Option("backend")
    @Description("These options affect the behavior of the proxy related to server connections.")
    private Backend backend = new Backend();

    public static ViaProxyConfig create(final File configFile) {
        final ConfigLoader<ViaProxyConfig> configLoader = new ConfigLoader<>(ViaProxyConfig.class);
        configLoader.getConfigOptions().setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1);
        try {
            return configLoader.load(ConfigProvider.file(configFile)).getConfigInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public void save() {
        try {
            this.configContext.save();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public Frontend getFrontend() {
        return this.frontend;
    }

    public Proxy getProxy() {
        return this.proxy;
    }

    public Backend getBackend() {
        return this.backend;
    }

    @Section
    public class Frontend {

        @Option("bind-address")
        @CLIName(value = "bind-address", omitSection = true)
        @Description("The address on which ViaProxy should listen for clients.")
        @TypeSerializer(SocketAddressTypeSerializer.class)
        private SocketAddress bindAddress = AddressUtil.parse("0.0.0.0:25568", null);

        @Option("online-mode")
        @CLIName(value = "frontend-online-mode", omitSection = true)
        @CLIAliases("proxy-online-mode")
        @Description({
                "Enabling Online Mode allows clients see skins on online mode servers and use the signed chat features.",
                "This requires clients to use a valid Minecraft account."
        })
        private boolean onlineMode = false;

        @Option("haproxy")
        @CLIName(value = "frontend-haproxy", omitSection = true)
        @Description("Read HAProxy protocol messages from clients.")
        private boolean haProxy = false;

        @Option("compression-threshold")
        @CLIName(value = "compression-threshold", omitSection = true)
        @Description("The threshold for packet compression. Packets larger than this size will be compressed. (-1 to disable)")
        private int compressionThreshold = 256;

        @Option("suppress-packet-errors")
        @CLIName(value = "suppress-client-packet-errors", omitSection = true)
        @CLIAliases("suppress-client-protocol-errors")
        @Description({
                "Enabling this will suppress packet errors to prevent lag when ViaProxy is getting spammed with invalid packets.",
                "This may cause issues with debugging client connection issues because no error messages will be printed."
        })
        private boolean suppressPacketErrors = false;

        @Option("motd")
        @Description("These options allow you to override parts of the backend server's MotD.")
        private MotD motd = new MotD();

        @Option("resource-pack-url")
        @CLIName(value = "resource-pack-url", omitSection = true)
        @Description({
                "URL of a resource pack which will be sent to clients. Leave empty to disable.",
                "Example: https://example.com/resourcepack.zip"
        })
        private String resourcePackUrl = "";

        @Option("wildcard-domain-handling")
        @CLIName(value = "wildcard-domain-handling", omitSection = true)
        @Description({
                "Allows clients to specify a target server and version using wildcard domains.",
                "none: No wildcard domain handling.",
                "public: Public wildcard domain handling. Intended for usage by external clients. (Example: address_port_version.viaproxy.127.0.0.1.nip.io)",
                "internal: Internal wildcard domain handling. Intended for local usage by custom clients. (Example: original-handshake-address\\7address:port\\7version)"
        })
        private WildcardDomainHandling wildcardDomainHandling = WildcardDomainHandling.NONE;

        @Option("log-client-status-requests")
        @CLIName(value = "log-client-status-requests", omitSection = true)
        @Description("Enable this if you want to see client status requests in the console and log files.")
        private boolean logClientStatusRequests = false;

        public SocketAddress getBindAddress() {
            return this.bindAddress;
        }

        public void setBindAddress(final SocketAddress bindAddress) {
            this.bindAddress = bindAddress;
            ViaProxyConfig.this.save();
        }

        public boolean isOnlineMode() {
            return this.onlineMode;
        }

        public void setOnlineMode(final boolean onlineMode) {
            this.onlineMode = onlineMode;
            ViaProxyConfig.this.save();
        }

        public boolean useHaProxy() {
            return this.haProxy;
        }

        public void setHaProxy(final boolean haProxy) {
            this.haProxy = haProxy;
            ViaProxyConfig.this.save();
        }

        public int getCompressionThreshold() {
            return this.compressionThreshold;
        }

        public void setCompressionThreshold(final int compressionThreshold) {
            this.compressionThreshold = compressionThreshold;
            ViaProxyConfig.this.save();
        }

        public boolean shouldSuppressPacketErrors() {
            return this.suppressPacketErrors;
        }

        public void setSuppressPacketErrors(final boolean suppressPacketErrors) {
            this.suppressPacketErrors = suppressPacketErrors;
            ViaProxyConfig.this.save();
        }

        public MotD getMotd() {
            return this.motd;
        }

        public String getResourcePackUrl() {
            return this.resourcePackUrl;
        }

        public void setResourcePackUrl(final String resourcePackUrl) {
            this.resourcePackUrl = resourcePackUrl;
            ViaProxyConfig.this.save();
        }

        public WildcardDomainHandling getWildcardDomainHandling() {
            return this.wildcardDomainHandling;
        }

        public void setWildcardDomainHandling(final WildcardDomainHandling wildcardDomainHandling) {
            this.wildcardDomainHandling = wildcardDomainHandling;
            ViaProxyConfig.this.save();
        }

        public boolean shouldLogClientStatusRequests() {
            return this.logClientStatusRequests;
        }

        public void setLogClientStatusRequests(final boolean logClientStatusRequests) {
            this.logClientStatusRequests = logClientStatusRequests;
            ViaProxyConfig.this.save();
        }

        @Section
        public class MotD {

            @Option("description")
            @CLIName(value = "custom-motd-description", omitSection = true)
            @CLIAliases("custom-motd")
            @Description("Custom description. Leave empty to use the backend server's description.")
            private String description = "";

            @Option("favicon-path")
            @CLIName(value = "custom-motd-favicon-path", omitSection = true)
            @CLIAliases("custom-favicon-path")
            @Description("Relative file path to a custom favicon. Leave empty to use the backend server's favicon.")
            private String faviconPath = "";

            public boolean hasAnyNonBlankOption() {
                return !this.description.isBlank() || !this.faviconPath.isBlank();
            }

            public String getDescription() {
                return this.description;
            }

            public void setDescription(final String description) {
                this.description = description;
                ViaProxyConfig.this.save();
            }

            public String getFaviconPath() {
                return this.faviconPath;
            }

            public void setFaviconPath(final String faviconPath) {
                this.faviconPath = faviconPath;
                ViaProxyConfig.this.save();
            }

        }

    }

    @Section
    public class Proxy {

        @Option("minecraft-account-index")
        @CLIName(value = "minecraft-account-index", omitSection = true)
        @Description("The GUI account list index (0 indexed) of the selected account.")
        @TypeSerializer(AccountTypeSerializer.class)
        private Account account = null;

        @Option("bungeecord-player-info-passthrough")
        @CLIName(value = "bungeecord-player-info-passthrough", omitSection = true)
        @Description({
                "Allow additional information like player ip, player uuid to be passed through to the backend server.",
                "This is typically used by proxies like BungeeCord and requires support from the backend server as well."
        })
        private boolean bungeecordPlayerInfoPassthrough = false;

        @Option("ignore-protocol-translation-errors")
        @CLIName(value = "ignore-protocol-translation-errors", omitSection = true)
        @Description({
                "Enabling this will prevent getting disconnected from the backend server when a packet translation error occurs and instead only print the error in the console.",
                "This may cause issues depending on the type of packet which failed to translate."
        })
        private boolean ignoreProtocolTranslationErrors = false;

        @Option("allow-legacy-client-passthrough")
        @CLIName(value = "allow-legacy-client-passthrough", omitSection = true)
        @Description("Allow <= 1.6.4 clients to connect through ViaProxy to the backend server. (No protocol translation or packet handling)")
        private boolean allowLegacyClientPassthrough = false;

        @Option("chat-signing")
        @CLIName(value = "chat-signing", omitSection = true)
        @Description("Enables sending signed chat messages on >= 1.19 servers.")
        private boolean chatSigning = true;

        @Option("rewrite-handshake-packet")
        @CLIName(value = "rewrite-handshake-packet", omitSection = true)
        @Description({
                "Enabling this will rewrite the address in the handshake packet to a value the vanilla client would have sent when connecting directly to the backend server.",
                "This should be left enabled unless you are a server owner and you need the original address on the backend server."
        })
        private boolean rewriteHandshakePacket = true;

        @Option("rewrite-transfer-packets")
        @CLIName(value = "rewrite-transfer-packets", omitSection = true)
        @Description({
                "Enabling this will rewrite transfer packets to point back to ViaProxy. This allows ViaProxy to perform protocol translation when forwarding the player to the actual server from the transfer packet.",
                "This should be left enabled unless you are a server owner and the servers you are transferring to perform their own protocol translation."
        })
        private boolean rewriteTransferPackets = true;

        @Option("fake-accept-resource-packs")
        @CLIName(value = "fake-accept-resource-packs", omitSection = true)
        @Description({
                "Accepts resource packs from the server without showing a prompt to clients.",
                "This is required if the server requires a resource pack, but the client can't load it due to version differences."
        })
        private boolean fakeAcceptResourcePacks = false;

        @Option("simple-voice-chat-support")
        @CLIName(value = "simple-voice-chat-support", omitSection = true)
        @Description("Enables handling and rewriting of Simple Voice Chat mod packets.")
        private boolean simpleVoiceChatSupport = false;

        @Option("fix-fabric-particle-api")
        @CLIName(value = "fix-fabric-particle-api", omitSection = true)
        @Description({
                "Fixes an issue where the Fabric Particle API causes disconnects when both the client and server have the mod installed and both are 1.21.5+.",
                "See https://github.com/ViaVersion/ViaFabric/issues/428"
        })
        private boolean fixFabricParticleApi = true;

        @Option("skip-config-state-packet-queue")
        @CLIName(value = "skip-config-state-packet-queue", omitSection = true)
        @Description({
                "Fixes potential join issues on <= 1.20.1 quilt/fabric servers.",
                "It's recommended to only enable this if you are experiencing issues with the config state packet queue."
        })
        private boolean skipConfigStatePacketQueue = false;

        @Option("log-ips")
        @CLIName(value = "log-ips", omitSection = true)
        @Description("Disable this if you want to hide IP addresses in the console and log files.")
        private boolean logIps = true;

        public Account getAccount() {
            return this.account;
        }

        public void setAccount(final Account account) {
            this.account = account;
            ViaProxyConfig.this.save();
        }

        public boolean shouldPassthroughBungeecordPlayerInfo() {
            return this.bungeecordPlayerInfoPassthrough;
        }

        public void setPassthroughBungeecordPlayerInfo(final boolean bungeecordPlayerInfoPassthrough) {
            this.bungeecordPlayerInfoPassthrough = bungeecordPlayerInfoPassthrough;
            ViaProxyConfig.this.save();
        }

        public boolean shouldIgnoreProtocolTranslationErrors() {
            return this.ignoreProtocolTranslationErrors;
        }

        public void setIgnoreProtocolTranslationErrors(final boolean ignoreProtocolTranslationErrors) {
            this.ignoreProtocolTranslationErrors = ignoreProtocolTranslationErrors;
            ViaProxyConfig.this.save();
        }

        public boolean shouldAllowLegacyClientPassthrough() {
            return this.allowLegacyClientPassthrough;
        }

        public void setAllowLegacyClientPassthrough(final boolean allowLegacyClientPassthrough) {
            this.allowLegacyClientPassthrough = allowLegacyClientPassthrough;
            ViaProxyConfig.this.save();
        }

        public boolean useChatSigning() {
            return this.chatSigning;
        }

        public void setChatSigning(final boolean chatSigning) {
            this.chatSigning = chatSigning;
            ViaProxyConfig.this.save();
        }

        public boolean shouldRewriteHandshakePacket() {
            return this.rewriteHandshakePacket;
        }

        public void setRewriteHandshakePacket(final boolean rewriteHandshakePacket) {
            this.rewriteHandshakePacket = rewriteHandshakePacket;
            ViaProxyConfig.this.save();
        }

        public boolean shouldRewriteTransferPackets() {
            return this.rewriteTransferPackets;
        }

        public void setRewriteTransferPackets(final boolean rewriteTransferPackets) {
            this.rewriteTransferPackets = rewriteTransferPackets;
            ViaProxyConfig.this.save();
        }

        public boolean shouldFakeAcceptResourcePacks() {
            return this.fakeAcceptResourcePacks;
        }

        public void setFakeAcceptResourcePacks(final boolean fakeAcceptResourcePacks) {
            this.fakeAcceptResourcePacks = fakeAcceptResourcePacks;
            ViaProxyConfig.this.save();
        }

        public boolean shouldSupportSimpleVoiceChat() {
            return this.simpleVoiceChatSupport;
        }

        public void setSimpleVoiceChatSupport(final boolean simpleVoiceChatSupport) {
            this.simpleVoiceChatSupport = simpleVoiceChatSupport;
            ViaProxyConfig.this.save();
        }

        public boolean shouldFixFabricParticleApi() {
            return this.fixFabricParticleApi;
        }

        public void setFixFabricParticleApi(final boolean fixFabricParticleApi) {
            this.fixFabricParticleApi = fixFabricParticleApi;
            ViaProxyConfig.this.save();
        }

        public boolean shouldSkipConfigStatePacketQueue() {
            return this.skipConfigStatePacketQueue;
        }

        public void setSkipConfigStatePacketQueue(final boolean skipConfigStatePacketQueue) {
            this.skipConfigStatePacketQueue = skipConfigStatePacketQueue;
            ViaProxyConfig.this.save();
        }

        public boolean shouldLogIps() {
            return this.logIps;
        }

        public void setLogIps(final boolean logIps) {
            this.logIps = logIps;
            ViaProxyConfig.this.save();
        }

    }

    @Section
    public class Backend {

        @Option(value = "address", dependencies = "version")
        @CLIName(value = "backend-address", omitSection = true)
        @CLIAliases("target-address")
        @Description("The address of the server ViaProxy should connect to.")
        @TypeSerializer(BackendAddressTypeSerializer.class)
        private SocketAddress address = AddressUtil.parse("127.0.0.1:25565", null);

        @Option("version")
        @CLIName(value = "backend-version", omitSection = true)
        @CLIAliases("target-version")
        @Description("The version ViaProxy should translate to. (See ViaProxy GUI for a list of versions)")
        @TypeSerializer(ProtocolVersionTypeSerializer.class)
        private ProtocolVersion version = ProtocolTranslator.AUTO_DETECT_PROTOCOL;

        @Option("connect-timeout")
        @CLIName(value = "connect-timeout", omitSection = true)
        @Description("The connect timeout in milliseconds.")
        private int connectTimeout = 8000;

        @Option("auth-method")
        @CLIName(value = "auth-method", omitSection = true)
        @Description({
                "The authentication method to use for joining the server.",
                "none: No authentication (Offline mode)",
                "account: Use an account (Has to be configured in ViaProxy GUI)"
        })
        private AuthMethod authMethod = AuthMethod.NONE;

        @Option("betacraft-auth")
        @CLIName(value = "betacraft-auth", omitSection = true)
        @Description({
                "Use BetaCraft authentication for joining classic servers.",
                "This allows clients to join classic servers which have online mode enabled."
        })
        private boolean betaCraftAuth = false;

        @Option("allow-beta-pinging")
        @CLIName(value = "allow-beta-pinging", omitSection = true)
        @Description("Enabling this allows clients to ping <= b1.7.3 servers. This may cause issues if the server blocks too frequent connections.")
        private boolean allowBetaPinging = false;

        @Option("haproxy")
        @CLIName(value = "backend-haproxy", omitSection = true)
        @Description("Send HAProxy protocol messages to the server.")
        private boolean haProxy = false;

        @Option("proxy-url")
        @CLIName(value = "backend-proxy-url", omitSection = true)
        @Description({
                "URL of a SOCKS(4/5)/HTTP(S) proxy through which connections will be made. Leave empty to connect directly.",
                "Supported formats:",
                "- type://address:port",
                "- type://username:password@address:port"
        })
        @TypeSerializer(ProxyTypeSerializer.class)
        private net.raphimc.viaproxy.util.Proxy proxy = null;

        public SocketAddress getAddress() {
            return this.address;
        }

        public void setAddress(final SocketAddress address) {
            this.address = address;
            ViaProxyConfig.this.save();
        }

        public ProtocolVersion getVersion() {
            return this.version;
        }

        public void setVersion(final ProtocolVersion version) {
            this.version = version;
            ViaProxyConfig.this.save();
        }

        public int getConnectTimeout() {
            return this.connectTimeout;
        }

        public void setConnectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            ViaProxyConfig.this.save();
        }

        public AuthMethod getAuthMethod() {
            return this.authMethod;
        }

        public void setAuthMethod(final AuthMethod authMethod) {
            this.authMethod = authMethod;
            ViaProxyConfig.this.save();
        }

        public boolean useBetaCraftAuth() {
            return this.betaCraftAuth;
        }

        public void setBetaCraftAuth(final boolean betaCraftAuth) {
            this.betaCraftAuth = betaCraftAuth;
            ViaProxyConfig.this.save();
        }

        public boolean shouldAllowBetaPinging() {
            return this.allowBetaPinging;
        }

        public void setAllowBetaPinging(final boolean allowBetaPinging) {
            this.allowBetaPinging = allowBetaPinging;
            ViaProxyConfig.this.save();
        }

        public boolean useHaProxy() {
            return this.haProxy;
        }

        public void setHaProxy(final boolean haProxy) {
            this.haProxy = haProxy;
            ViaProxyConfig.this.save();
        }

        public net.raphimc.viaproxy.util.Proxy getProxy() {
            return this.proxy;
        }

        public void setProxy(final net.raphimc.viaproxy.util.Proxy proxy) {
            this.proxy = proxy;
            ViaProxyConfig.this.save();
        }

        @Validator("version")
        private ProtocolVersion validateVersion(final ProtocolVersion backendVersion) {
            if (backendVersion == null) {
                Logger.LOGGER.warn("Invalid backend version. Defaulting to auto detect.");
                Logger.LOGGER.warn("=== Supported Protocol Versions ===");
                for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                    Logger.LOGGER.warn(version.getName());
                }
                Logger.LOGGER.warn("===================================");
                return ProtocolTranslator.AUTO_DETECT_PROTOCOL;
            }
            return backendVersion;
        }

    }

    public enum AuthMethod {

        /**
         * Use an account (Has to be configured in ViaProxy GUI)
         */
        ACCOUNT("tab.general.minecraft_account.option_select_account"),
        /**
         * No authentication (Offline mode)
         */
        NONE("tab.general.minecraft_account.option_no_account");

        private final String guiTranslationKey;

        AuthMethod(final String guiTranslationKey) {
            this.guiTranslationKey = guiTranslationKey;
        }

        public String getGuiTranslationKey() {
            return this.guiTranslationKey;
        }

    }

    public enum WildcardDomainHandling {

        /**
         * No wildcard domain handling
         */
        NONE,
        /**
         * Public wildcard domain handling
         */
        PUBLIC,
        /**
         * Internal wildcard domain handling
         */
        INTERNAL,

    }

    static class Migrator1To2 implements ConfigMigrator {

        @Override
        public void migrate(final int currentVersion, final Map<String, Object> loadedValues) {
            this.move(loadedValues, "bind-address", "frontend", "bind-address");
            this.move(loadedValues, "target-address", "backend", "address");
            this.move(loadedValues, "target-version", "backend", "version");
            this.move(loadedValues, "connect-timeout", "backend", "connect-timeout");
            this.move(loadedValues, "proxy-online-mode", "frontend", "online-mode");
            this.move(loadedValues, "auth-method", "backend", "auth-method");
            this.move(loadedValues, "minecraft-account-index", "proxy", "minecraft-account-index");
            this.move(loadedValues, "betacraft-auth", "backend", "betacraft-auth");
            this.move(loadedValues, "backend-proxy-url", "backend", "proxy-url");
            this.move(loadedValues, "backend-haproxy", "backend", "haproxy");
            this.move(loadedValues, "frontend-haproxy", "frontend", "haproxy");
            this.move(loadedValues, "chat-signing", "proxy", "chat-signing");
            this.move(loadedValues, "compression-threshold", "frontend", "compression-threshold");
            this.move(loadedValues, "allow-beta-pinging", "backend", "allow-beta-pinging");
            this.move(loadedValues, "ignore-protocol-translation-errors", "proxy", "ignore-protocol-translation-errors");
            this.move(loadedValues, "suppress-client-protocol-errors", "proxy", "suppress-packet-errors");
            this.move(loadedValues, "allow-legacy-client-passthrough", "proxy", "allow-legacy-client-passthrough");
            this.move(loadedValues, "bungeecord-player-info-passthrough", "proxy", "bungeecord-player-info-passthrough");
            this.move(loadedValues, "rewrite-handshake-packet", "proxy", "rewrite-handshake-packet");
            this.move(loadedValues, "rewrite-transfer-packets", "proxy", "rewrite-transfer-packets");
            this.move(loadedValues, "custom-motd", "frontend", "motd", "description");
            this.move(loadedValues, "custom-favicon-path", "frontend", "motd", "favicon-path");
            this.move(loadedValues, "resource-pack-url", "frontend", "resource-pack-url");
            this.move(loadedValues, "wildcard-domain-handling", "frontend", "wildcard-domain-handling");
            this.move(loadedValues, "simple-voice-chat-support", "proxy", "simple-voice-chat-support");
            this.move(loadedValues, "fix-fabric-particle-api", "proxy", "fix-fabric-particle-api");
            this.move(loadedValues, "fake-accept-resource-packs", "proxy", "fake-accept-resource-packs");
            this.move(loadedValues, "skip-config-state-packet-queue", "proxy", "skip-config-state-packet-queue");
            this.move(loadedValues, "log-ips", "proxy", "log-ips");
            this.move(loadedValues, "log-client-status-requests", "frontend", "log-client-status-requests");
        }

        private void move(final Map<String, Object> loadedValues, final String oldKey, final String newSection, final String newKey) {
            if (loadedValues.containsKey(oldKey)) {
                final Object value = loadedValues.remove(oldKey);
                final Map<String, Object> sectionMap = (Map<String, Object>) loadedValues.computeIfAbsent(newSection, k -> new HashMap<>());
                sectionMap.put(newKey, value);
            }
        }

        private void move(final Map<String, Object> loadedValues, final String oldKey, final String newSection, final String newSubSection, final String newKey) {
            if (loadedValues.containsKey(oldKey)) {
                final Object value = loadedValues.remove(oldKey);
                final Map<String, Object> sectionMap = (Map<String, Object>) loadedValues.computeIfAbsent(newSection, k -> new HashMap<>());
                final Map<String, Object> subSectionMap = (Map<String, Object>) sectionMap.computeIfAbsent(newSubSection, k -> new HashMap<>());
                subSectionMap.put(newKey, value);
            }
        }

    }

    @Deprecated(forRemoval = true)
    public SocketAddress getBindAddress() {
        return this.getFrontend().getBindAddress();
    }

    @Deprecated(forRemoval = true)
    public void setBindAddress(final SocketAddress bindAddress) {
        this.getFrontend().setBindAddress(bindAddress);
    }

    @Deprecated(forRemoval = true)
    public SocketAddress getTargetAddress() {
        return this.getBackend().getAddress();
    }

    @Deprecated(forRemoval = true)
    public void setTargetAddress(final SocketAddress targetAddress) {
        this.getBackend().setAddress(targetAddress);
    }

    @Deprecated(forRemoval = true)
    public ProtocolVersion getTargetVersion() {
        return this.getBackend().getVersion();
    }

    @Deprecated(forRemoval = true)
    public void setTargetVersion(final ProtocolVersion targetVersion) {
        this.getBackend().setVersion(targetVersion);
    }

    @Deprecated(forRemoval = true)
    public int getConnectTimeout() {
        return this.getBackend().getConnectTimeout();
    }

    @Deprecated(forRemoval = true)
    public void setConnectTimeout(final int connectTimeout) {
        this.getBackend().setConnectTimeout(connectTimeout);
    }

    @Deprecated(forRemoval = true)
    public boolean isProxyOnlineMode() {
        return this.getFrontend().isOnlineMode();
    }

    @Deprecated(forRemoval = true)
    public void setProxyOnlineMode(final boolean proxyOnlineMode) {
        this.getFrontend().setOnlineMode(proxyOnlineMode);
    }

    @Deprecated(forRemoval = true)
    public AuthMethod getAuthMethod() {
        return this.getBackend().getAuthMethod();
    }

    @Deprecated(forRemoval = true)
    public void setAuthMethod(final AuthMethod authMethod) {
        this.getBackend().setAuthMethod(authMethod);
    }

    @Deprecated(forRemoval = true)
    public Account getAccount() {
        return this.getProxy().getAccount();
    }

    @Deprecated(forRemoval = true)
    public void setAccount(final Account account) {
        this.getProxy().setAccount(account);
    }

    @Deprecated(forRemoval = true)
    public boolean useBetacraftAuth() {
        return this.getBackend().useBetaCraftAuth();
    }

    @Deprecated(forRemoval = true)
    public void setBetacraftAuth(final boolean betacraftAuth) {
        this.getBackend().setBetaCraftAuth(betacraftAuth);
    }

    @Deprecated(forRemoval = true)
    public net.raphimc.viaproxy.util.Proxy getBackendProxy() {
        return this.getBackend().getProxy();
    }

    @Deprecated(forRemoval = true)
    public void setBackendProxy(final net.raphimc.viaproxy.util.Proxy backendProxy) {
        this.getBackend().setProxy(backendProxy);
    }

    @Deprecated(forRemoval = true)
    public boolean useBackendHaProxy() {
        return this.getBackend().useHaProxy();
    }

    @Deprecated(forRemoval = true)
    public void setBackendHaProxy(final boolean backendHaProxy) {
        this.getBackend().setHaProxy(backendHaProxy);
    }

    @Deprecated(forRemoval = true)
    public boolean useFrontendHaProxy() {
        return this.getFrontend().useHaProxy();
    }

    @Deprecated(forRemoval = true)
    public void setFrontendHaProxy(final boolean frontendHaProxy) {
        this.getFrontend().setHaProxy(frontendHaProxy);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldSignChat() {
        return this.getProxy().useChatSigning();
    }

    @Deprecated(forRemoval = true)
    public void setChatSigning(final boolean chatSigning) {
        this.getProxy().setChatSigning(chatSigning);
    }

    @Deprecated(forRemoval = true)
    public int getCompressionThreshold() {
        return this.getFrontend().getCompressionThreshold();
    }

    @Deprecated(forRemoval = true)
    public void setCompressionThreshold(final int compressionThreshold) {
        this.getFrontend().setCompressionThreshold(compressionThreshold);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldAllowBetaPinging() {
        return this.getBackend().shouldAllowBetaPinging();
    }

    @Deprecated(forRemoval = true)
    public void setAllowBetaPinging(final boolean allowBetaPinging) {
        this.getBackend().setAllowBetaPinging(allowBetaPinging);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldIgnoreProtocolTranslationErrors() {
        return this.getProxy().shouldIgnoreProtocolTranslationErrors();
    }

    @Deprecated(forRemoval = true)
    public void setIgnoreProtocolTranslationErrors(final boolean ignoreProtocolTranslationErrors) {
        this.getProxy().setIgnoreProtocolTranslationErrors(ignoreProtocolTranslationErrors);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldSuppressClientProtocolErrors() {
        return this.getFrontend().shouldSuppressPacketErrors();
    }

    @Deprecated(forRemoval = true)
    public void setSuppressClientProtocolErrors(final boolean suppressClientProtocolErrors) {
        this.getFrontend().setSuppressPacketErrors(suppressClientProtocolErrors);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldAllowLegacyClientPassthrough() {
        return this.getProxy().shouldAllowLegacyClientPassthrough();
    }

    @Deprecated(forRemoval = true)
    public void setAllowLegacyClientPassthrough(final boolean allowLegacyClientPassthrough) {
        this.getProxy().setAllowLegacyClientPassthrough(allowLegacyClientPassthrough);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldPassthroughBungeecordPlayerInfo() {
        return this.getProxy().shouldPassthroughBungeecordPlayerInfo();
    }

    @Deprecated(forRemoval = true)
    public void setPassthroughBungeecordPlayerInfo(final boolean bungeecordPlayerInfoPassthrough) {
        this.getProxy().setPassthroughBungeecordPlayerInfo(bungeecordPlayerInfoPassthrough);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldRewriteHandshakePacket() {
        return this.getProxy().shouldRewriteHandshakePacket();
    }

    @Deprecated(forRemoval = true)
    public void setRewriteHandshakePacket(final boolean rewriteHandshakePacket) {
        this.getProxy().setRewriteHandshakePacket(rewriteHandshakePacket);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldRewriteTransferPackets() {
        return this.getProxy().shouldRewriteTransferPackets();
    }

    @Deprecated(forRemoval = true)
    public void setRewriteTransferPackets(final boolean rewriteTransferPackets) {
        this.getProxy().setRewriteTransferPackets(rewriteTransferPackets);
    }

    @Deprecated(forRemoval = true)
    public String getCustomMotd() {
        return this.getFrontend().getMotd().getDescription();
    }

    @Deprecated(forRemoval = true)
    public void setCustomMotd(final String customMotd) {
        this.getFrontend().getMotd().setDescription(customMotd);
    }

    @Deprecated(forRemoval = true)
    public String getCustomFaviconPath() {
        return this.getFrontend().getMotd().getFaviconPath();
    }

    @Deprecated(forRemoval = true)
    public void setCustomFaviconPath(final String customFaviconPath) {
        this.getFrontend().getMotd().setFaviconPath(customFaviconPath);
    }

    @Deprecated(forRemoval = true)
    public String getResourcePackUrl() {
        return this.getFrontend().getResourcePackUrl();
    }

    @Deprecated(forRemoval = true)
    public void setResourcePackUrl(final String resourcePackUrl) {
        this.getFrontend().setResourcePackUrl(resourcePackUrl);
    }

    @Deprecated(forRemoval = true)
    public WildcardDomainHandling getWildcardDomainHandling() {
        return this.getFrontend().getWildcardDomainHandling();
    }

    @Deprecated(forRemoval = true)
    public void setWildcardDomainHandling(final WildcardDomainHandling wildcardDomainHandling) {
        this.getFrontend().setWildcardDomainHandling(wildcardDomainHandling);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldSupportSimpleVoiceChat() {
        return this.getProxy().shouldSupportSimpleVoiceChat();
    }

    @Deprecated(forRemoval = true)
    public void setSimpleVoiceChatSupport(final boolean simpleVoiceChatSupport) {
        this.getProxy().setSimpleVoiceChatSupport(simpleVoiceChatSupport);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldFixFabricParticleApi() {
        return this.getProxy().shouldFixFabricParticleApi();
    }

    @Deprecated(forRemoval = true)
    public void setFixFabricParticleApi(final boolean fixFabricParticleApi) {
        this.getProxy().setFixFabricParticleApi(fixFabricParticleApi);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldFakeAcceptResourcePacks() {
        return this.getProxy().shouldFakeAcceptResourcePacks();
    }

    @Deprecated(forRemoval = true)
    public void setFakeAcceptResourcePacks(final boolean fakeAcceptResourcePacks) {
        this.getProxy().setFakeAcceptResourcePacks(fakeAcceptResourcePacks);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldSkipConfigStatePacketQueue() {
        return this.getProxy().shouldSkipConfigStatePacketQueue();
    }

    @Deprecated(forRemoval = true)
    public void setSkipConfigStatePacketQueue(final boolean skipConfigStatePacketQueue) {
        this.getProxy().setSkipConfigStatePacketQueue(skipConfigStatePacketQueue);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldLogIps() {
        return this.getProxy().shouldLogIps();
    }

    @Deprecated(forRemoval = true)
    public void setLogIps(final boolean logIps) {
        this.getProxy().setLogIps(logIps);
    }

    @Deprecated(forRemoval = true)
    public boolean shouldLogClientStatusRequests() {
        return this.getFrontend().shouldLogClientStatusRequests();
    }

    @Deprecated(forRemoval = true)
    public void setLogClientStatusRequests(final boolean logClientStatusRequests) {
        this.getFrontend().setLogClientStatusRequests(logClientStatusRequests);
    }

}
