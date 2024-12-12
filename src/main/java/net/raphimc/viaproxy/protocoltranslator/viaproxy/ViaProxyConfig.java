/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2024 RK_01/RaphiMC and contributors
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
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.lenni0451.optconfig.ConfigContext;
import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.annotations.*;
import net.lenni0451.optconfig.index.ClassIndexer;
import net.lenni0451.optconfig.index.ConfigType;
import net.lenni0451.optconfig.index.types.ConfigOption;
import net.lenni0451.optconfig.index.types.SectionIndex;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.BetterHelpFormatter;
import net.raphimc.viaproxy.cli.HelpRequestedException;
import net.raphimc.viaproxy.plugins.events.PostOptionsParseEvent;
import net.raphimc.viaproxy.plugins.events.PreOptionsParseEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.config.*;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.net.SocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

@OptConfig(header = "ViaProxy configuration file", version = 1)
public class ViaProxyConfig {

    private ConfigContext<ViaProxyConfig> configContext;

    @NotReloadable
    @Option("bind-address")
    @Description("The address ViaProxy should listen for connections.")
    @TypeSerializer(SocketAddressTypeSerializer.class)
    private SocketAddress bindAddress = AddressUtil.parse("0.0.0.0:25568", null);

    @Option(value = "target-address", dependencies = "target-version")
    @Description("The address of the server ViaProxy should connect to.")
    @TypeSerializer(TargetAddressTypeSerializer.class)
    private SocketAddress targetAddress = AddressUtil.parse("127.0.0.1:25565", null);

    @Option("target-version")
    @Description("The version ViaProxy should translate to. (See ViaProxy GUI for a list of versions)")
    @TypeSerializer(ProtocolVersionTypeSerializer.class)
    private ProtocolVersion targetVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL;

    @Option("connect-timeout")
    @Description("The connect timeout for backend server connections in milliseconds.")
    private int connectTimeout = 8000;

    @Option("proxy-online-mode")
    @Description({
            "Proxy Online Mode allows you to see skins on online mode servers and use the signed chat features.",
            "Enabling Proxy Online Mode requires your client to have a valid minecraft account."
    })
    private boolean proxyOnlineMode = false;

    @Option("auth-method")
    @Description({
            "The authentication method to use for joining the target server.",
            "none: No authentication (Offline mode)",
            "openauthmod: Requires the OpenAuthMod client mod (https://modrinth.com/mod/openauthmod)",
            "account: Use an account for joining the target server. (Has to be configured in ViaProxy GUI)"
    })
    private AuthMethod authMethod = AuthMethod.NONE;

    @Option(value = "minecraft-account-index", dependencies = "auth-method")
    @Description("The GUI account list index (0 indexed) of the account if the auth method is set to account.")
    @TypeSerializer(AccountTypeSerializer.class)
    private Account account = null;

    @Option("betacraft-auth")
    @Description({
            "Use BetaCraft authentication for classic servers.",
            "Enabling BetaCraft Auth allows you to join classic servers which have online mode enabled."
    })
    private boolean betacraftAuth = false;

    @Option("backend-proxy-url")
    @Description({
            "URL of a SOCKS(4/5)/HTTP(S) proxy which will be used for backend server connections. Leave empty to connect directly.",
            "Supported formats:",
            "- type://address:port",
            "- type://username:password@address:port"
    })
    @TypeSerializer(ProxyUriTypeSerializer.class)
    private URI backendProxyUrl = null;

    @Option("backend-haproxy")
    @Description("Send HAProxy protocol messages to the target server.")
    private boolean backendHaProxy = false;

    @Option("frontend-haproxy")
    @Description("Read HAProxy protocol messages from client connections.")
    private boolean frontendHaProxy = false;

    @Option("chat-signing")
    @Description("Enables sending signed chat messages on >= 1.19 servers.")
    private boolean chatSigning = true;

    @Option("compression-threshold")
    @Description("The threshold for packet compression. Packets larger than this size will be compressed. (-1 to disable)")
    private int compressionThreshold = 256;

    @Option("allow-beta-pinging")
    @Description("Enabling this will allow you to ping <= b1.7.3 servers. This may cause issues with servers that block too frequent connections.")
    private boolean allowBetaPinging = false;

    @Option("ignore-protocol-translation-errors")
    @Description({
            "Enabling this will prevent getting disconnected from the server when a packet translation error occurs and instead only print the error in the console.",
            "This may cause issues depending on the type of packet which failed to translate."
    })
    private boolean ignoreProtocolTranslationErrors = false;

    @Option("suppress-client-protocol-errors")
    @Description({
            "Enabling this will suppress client protocol errors to prevent lag when ViaProxy is getting spammed with invalid packets.",
            "This may cause issues with debugging client connection issues because no error messages will be printed."
    })
    private boolean suppressClientProtocolErrors = false;

    @Option("allow-legacy-client-passthrough")
    @Description("Allow <= 1.6.4 clients to connect through ViaProxy to the target server. (No protocol translation or packet handling)")
    private boolean allowLegacyClientPassthrough = false;

    @Option("bungeecord-player-info-passthrough")
    @Description({
            "Allow additional information like player ip, player uuid to be passed through to the backend server.",
            "This is typically used by proxies like BungeeCord and requires support from the backend server as well."
    })
    private boolean bungeecordPlayerInfoPassthrough = false;

    @Option("custom-motd")
    @Description("Custom MOTD to send when clients ping the proxy. Leave empty to use the target server's MOTD.")
    private String customMotd = "";

    @Option("resource-pack-url")
    @Description({"URL of a resource pack which clients can optionally download when connecting to the server. Leave empty to disable.", "Example: http://example.com/resourcepack.zip"})
    private String resourcePackUrl = "";

    @Option("wildcard-domain-handling")
    @Description({
            "Allows clients to specify a target server and version using wildcard domains.",
            "none: No wildcard domain handling.",
            "public: Public wildcard domain handling. Intended for usage by external clients. (Example: address_port_version.viaproxy.127.0.0.1.nip.io)",
            "internal: Internal wildcard domain handling. Intended for local usage by custom clients. (Example: original-handshake-address\\7address:port\\7version\\7classic-mppass)"
    })
    private WildcardDomainHandling wildcardDomainHandling = WildcardDomainHandling.NONE;

    @Option("simple-voice-chat-support")
    @Description("Enables handling and rewriting of Simple Voice Chat mod packets.")
    private boolean simpleVoiceChatSupport = false;

    @Option("fake-accept-resource-packs")
    @Description({
            "Accepts resource packs from the server without showing a prompt to the client.",
            "This is required for servers that require a resource pack, but the client can't load it due to version differences."
    })
    private boolean fakeAcceptResourcePacks = false;

    public static ViaProxyConfig create(final File configFile) {
        final ConfigLoader<ViaProxyConfig> configLoader = new ConfigLoader<>(ViaProxyConfig.class);
        configLoader.getConfigOptions().setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1);
        try {
            return configLoader.load(ConfigProvider.file(configFile)).getConfigInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public void loadFromArguments(final String[] args) throws Exception {
        final OptionParser optionParser = new OptionParser();
        final OptionSpec<Void> optionHelp = optionParser.accepts("help").forHelp();
        final OptionSpec<Void> optionListVersions = optionParser.accepts("list-versions", "Lists all supported server/target versions").forHelp();

        final Map<OptionSpec<Object>, ConfigOption> optionMap = new HashMap<>();
        final Stack<SectionIndex> stack = new Stack<>();
        final ConfigLoader<ViaProxyConfig> configLoader = new ConfigLoader<>(ViaProxyConfig.class);
        stack.push(ClassIndexer.indexClass(ConfigType.INSTANCED, ViaProxyConfig.class, configLoader.getConfigOptions().getClassAccessFactory()));
        while (!stack.isEmpty()) {
            final SectionIndex index = stack.pop();
            stack.addAll(index.getSubSections().values());

            for (ConfigOption option : index.getOptions()) {
                if (index.getSubSections().containsKey(option)) continue;

                Object defaultValue = option.getFieldAccess().getValue(this);
                if (option.getTypeSerializer() != null) {
                    defaultValue = option.createTypeSerializer(configLoader, ViaProxyConfig.class, this).serialize(defaultValue);
                }
                final OptionSpec<Object> cliOption = optionParser.accepts(option.getName()).withRequiredArg().ofType((Class<Object>) defaultValue.getClass()).defaultsTo(defaultValue);
                optionMap.put(cliOption, option);
            }
        }

        try {
            ViaProxy.EVENT_MANAGER.call(new PreOptionsParseEvent(optionParser));
            final OptionSet options = optionParser.parse(args);
            if (options.has(optionHelp)) {
                throw new HelpRequestedException();
            } else if (options.has(optionListVersions)) {
                Logger.LOGGER.info("=== Supported Server Versions ===");
                for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                    Logger.LOGGER.info(version.getName());
                }
                Logger.LOGGER.info("===================================");
                System.exit(0);
            }

            if (options.has("minecraft-account-index")) {
                this.authMethod = AuthMethod.ACCOUNT;
            }
            for (Map.Entry<OptionSpec<Object>, ConfigOption> entry : optionMap.entrySet()) {
                final ConfigOption option = entry.getValue();
                if (options.has(entry.getKey())) {
                    Object value = options.valueOf(entry.getKey());
                    if (option.getTypeSerializer() != null) {
                        value = option.createTypeSerializer(configLoader, ViaProxyConfig.class, this).deserialize((Class<Object>) option.getFieldAccess().getType(), value);
                    }
                    if (option.getValidator() != null) {
                        value = option.getValidator().invoke(this, value);
                    }
                    option.getFieldAccess().setValue(this, value);
                }
            }

            ViaProxy.EVENT_MANAGER.call(new PostOptionsParseEvent(options));
            return;
        } catch (OptionException e) {
            Logger.LOGGER.fatal("Error parsing CLI options: " + e.getMessage());
        } catch (HelpRequestedException ignored) {
        }

        optionParser.formatHelpWith(new BetterHelpFormatter());
        optionParser.printHelpOn(Logger.SYSOUT);
        Logger.LOGGER.info("For a more detailed description of the options, please refer to the viaproxy.yml file.");
        System.exit(1);
    }

    public void save() {
        try {
            this.configContext.save();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to save config", e);
        }
    }

    public SocketAddress getBindAddress() {
        return this.bindAddress;
    }

    public void setBindAddress(final SocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        this.save();
    }

    public SocketAddress getTargetAddress() {
        return this.targetAddress;
    }

    public void setTargetAddress(final SocketAddress targetAddress) {
        this.targetAddress = targetAddress;
        this.save();
    }

    public ProtocolVersion getTargetVersion() {
        return this.targetVersion;
    }

    public void setTargetVersion(final ProtocolVersion targetVersion) {
        this.targetVersion = targetVersion;
        this.save();
    }

    public int getConnectTimeout() {
        return this.connectTimeout;
    }

    public void setConnectTimeout(final int connectTimeout) {
        this.connectTimeout = connectTimeout;
        this.save();
    }

    public boolean isProxyOnlineMode() {
        return this.proxyOnlineMode;
    }

    public void setProxyOnlineMode(final boolean proxyOnlineMode) {
        this.proxyOnlineMode = proxyOnlineMode;
        this.save();
    }

    public AuthMethod getAuthMethod() {
        return this.authMethod;
    }

    public void setAuthMethod(final AuthMethod authMethod) {
        this.authMethod = authMethod;
        this.save();
    }

    public Account getAccount() {
        return this.account;
    }

    public void setAccount(final Account account) {
        this.account = account;
        this.save();
    }

    public boolean useBetacraftAuth() {
        return this.betacraftAuth;
    }

    public void setBetacraftAuth(final boolean betacraftAuth) {
        this.betacraftAuth = betacraftAuth;
        this.save();
    }

    public URI getBackendProxyUrl() {
        return this.backendProxyUrl;
    }

    public void setBackendProxyUrl(final URI backendProxyUrl) {
        this.backendProxyUrl = backendProxyUrl;
        this.save();
    }

    public boolean useBackendHaProxy() {
        return this.backendHaProxy;
    }

    public void setBackendHaProxy(final boolean backendHaProxy) {
        this.backendHaProxy = backendHaProxy;
        this.save();
    }

    public boolean useFrontendHaProxy() {
        return this.frontendHaProxy;
    }

    public void setFrontendHaProxy(final boolean frontendHaProxy) {
        this.frontendHaProxy = frontendHaProxy;
        this.save();
    }

    public boolean shouldSignChat() {
        return this.chatSigning;
    }

    public void setChatSigning(final boolean chatSigning) {
        this.chatSigning = chatSigning;
        this.save();
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }

    public void setCompressionThreshold(final int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
        this.save();
    }

    public boolean shouldAllowBetaPinging() {
        return this.allowBetaPinging;
    }

    public void setAllowBetaPinging(final boolean allowBetaPinging) {
        this.allowBetaPinging = allowBetaPinging;
        this.save();
    }

    public boolean shouldIgnoreProtocolTranslationErrors() {
        return this.ignoreProtocolTranslationErrors;
    }

    public void setIgnoreProtocolTranslationErrors(final boolean ignoreProtocolTranslationErrors) {
        this.ignoreProtocolTranslationErrors = ignoreProtocolTranslationErrors;
        this.save();
    }

    public boolean shouldSuppressClientProtocolErrors() {
        return this.suppressClientProtocolErrors;
    }

    public void setSuppressClientProtocolErrors(final boolean suppressClientProtocolErrors) {
        this.suppressClientProtocolErrors = suppressClientProtocolErrors;
        this.save();
    }

    public boolean shouldAllowLegacyClientPassthrough() {
        return this.allowLegacyClientPassthrough;
    }

    public void setAllowLegacyClientPassthrough(final boolean allowLegacyClientPassthrough) {
        this.allowLegacyClientPassthrough = allowLegacyClientPassthrough;
        this.save();
    }

    public boolean shouldPassthroughBungeecordPlayerInfo() {
        return this.bungeecordPlayerInfoPassthrough;
    }

    public void setPassthroughBungeecordPlayerInfo(final boolean bungeecordPlayerInfoPassthrough) {
        this.bungeecordPlayerInfoPassthrough = bungeecordPlayerInfoPassthrough;
        this.save();
    }

    public String getCustomMotd() {
        return this.customMotd;
    }

    public void setCustomMotd(final String customMotd) {
        this.customMotd = customMotd;
        this.save();
    }

    public String getResourcePackUrl() {
        return this.resourcePackUrl;
    }

    public void setResourcePackUrl(final String resourcePackUrl) {
        this.resourcePackUrl = resourcePackUrl;
        this.save();
    }

    public WildcardDomainHandling getWildcardDomainHandling() {
        return this.wildcardDomainHandling;
    }

    public void setWildcardDomainHandling(final WildcardDomainHandling wildcardDomainHandling) {
        this.wildcardDomainHandling = wildcardDomainHandling;
        this.save();
    }

    public boolean shouldSupportSimpleVoiceChat() {
        return this.simpleVoiceChatSupport;
    }

    public void setSimpleVoiceChatSupport(final boolean simpleVoiceChatSupport) {
        this.simpleVoiceChatSupport = simpleVoiceChatSupport;
        this.save();
    }

    public boolean shouldFakeAcceptResourcePacks() {
        return this.fakeAcceptResourcePacks;
    }

    public void setFakeAcceptResourcePacks(final boolean fakeAcceptResourcePacks) {
        this.fakeAcceptResourcePacks = fakeAcceptResourcePacks;
        this.save();
    }

    @Validator("target-version")
    private ProtocolVersion validateTargetVersion(final ProtocolVersion targetVersion) {
        if (targetVersion == null) {
            Logger.LOGGER.warn("Invalid target server version. Defaulting to auto detect.");
            Logger.LOGGER.warn("=== Supported Protocol Versions ===");
            for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                Logger.LOGGER.warn(version.getName());
            }
            Logger.LOGGER.warn("===================================");
            return ProtocolTranslator.AUTO_DETECT_PROTOCOL;
        }
        return targetVersion;
    }

    public enum AuthMethod {

        /**
         * Use an account for joining the target server (Has to be configured in ViaProxy GUI)
         */
        ACCOUNT("tab.general.minecraft_account.option_select_account"),
        /**
         * No authentication (Offline mode)
         */
        NONE("tab.general.minecraft_account.option_no_account");

        private final String guiTranslationKey;

        AuthMethod(String guiTranslationKey) {
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
         * Iternal wildcard domain handling
         */
        INTERNAL,

    }

}
