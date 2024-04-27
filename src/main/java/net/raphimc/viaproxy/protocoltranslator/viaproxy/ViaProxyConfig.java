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
import com.viaversion.viaversion.util.Config;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.BetterHelpFormatter;
import net.raphimc.viaproxy.cli.HelpRequestedException;
import net.raphimc.viaproxy.cli.ProtocolVersionConverter;
import net.raphimc.viaproxy.plugins.events.PostOptionsParseEvent;
import net.raphimc.viaproxy.plugins.events.PreOptionsParseEvent;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViaProxyConfig extends Config implements com.viaversion.viaversion.api.configuration.Config {

    private OptionParser optionParser;
    private final OptionSpec<Void> optionHelp;
    private final OptionSpec<String> optionBindAddress;
    private final OptionSpec<String> optionTargetAddress;
    private final OptionSpec<ProtocolVersion> optionTargetVersion;
    private final OptionSpec<Boolean> optionProxyOnlineMode;
    private final OptionSpec<AuthMethod> optionAuthMethod;
    private final OptionSpec<Integer> optionMinecraftAccountIndex;
    private final OptionSpec<Boolean> optionBetacraftAuth;
    private final OptionSpec<String> optionBackendProxyUrl;
    private final OptionSpec<Boolean> optionBackendHaProxy;
    private final OptionSpec<Boolean> optionChatSigning;
    private final OptionSpec<Integer> optionCompressionThreshold;
    private final OptionSpec<Boolean> optionAllowBetaPinging;
    private final OptionSpec<Boolean> optionIgnoreProtocolTranslationErrors;
    private final OptionSpec<Boolean> optionAllowLegacyClientPassthrough;
    private final OptionSpec<String> optionResourcePackUrl;
    private final OptionSpec<WildcardDomainHandling> optionWildcardDomainHandling;

    private SocketAddress bindAddress = AddressUtil.parse("0.0.0.0:25568", null);
    private SocketAddress targetAddress = AddressUtil.parse("127.0.0.1:25565", null);
    private ProtocolVersion targetVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL;
    private boolean proxyOnlineMode = false;
    private AuthMethod authMethod = AuthMethod.NONE;
    private Account account = null;
    private boolean betacraftAuth = false;
    private URI backendProxyUrl = null;
    private boolean backendHaProxy = false;
    private boolean chatSigning = true;
    private int compressionThreshold = 256;
    private boolean allowBetaPinging = false;
    private boolean ignoreProtocolTranslationErrors = false;
    private boolean allowLegacyClientPassthrough = false;
    private String resourcePackUrl = "";
    private WildcardDomainHandling wildcardDomainHandling = WildcardDomainHandling.NONE;

    public ViaProxyConfig(final File configFile) {
        super(configFile);

        this.optionParser = new OptionParser();
        this.optionHelp = this.optionParser.accepts("help").forHelp();
        this.optionBindAddress = this.optionParser.accepts("bind-address").withRequiredArg().ofType(String.class).defaultsTo(AddressUtil.toString(this.bindAddress));
        this.optionTargetAddress = this.optionParser.accepts("target-address").withRequiredArg().ofType(String.class).defaultsTo(AddressUtil.toString(this.targetAddress));
        this.optionTargetVersion = this.optionParser.accepts("target-version").withRequiredArg().withValuesConvertedBy(new ProtocolVersionConverter()).defaultsTo(this.targetVersion);
        this.optionProxyOnlineMode = this.optionParser.accepts("proxy-online-mode").withRequiredArg().ofType(Boolean.class).defaultsTo(this.proxyOnlineMode);
        this.optionAuthMethod = this.optionParser.accepts("auth-method").withRequiredArg().ofType(AuthMethod.class).defaultsTo(this.authMethod);
        this.optionMinecraftAccountIndex = this.optionParser.accepts("minecraft-account-index").withRequiredArg().ofType(Integer.class).defaultsTo(0);
        this.optionBetacraftAuth = this.optionParser.accepts("betacraft-auth").withRequiredArg().ofType(Boolean.class).defaultsTo(this.betacraftAuth);
        this.optionBackendProxyUrl = this.optionParser.accepts("backend-proxy-url").withRequiredArg().ofType(String.class).defaultsTo("");
        this.optionBackendHaProxy = this.optionParser.accepts("backend-haproxy").withRequiredArg().ofType(Boolean.class).defaultsTo(this.backendHaProxy);
        this.optionChatSigning = this.optionParser.accepts("chat-signing").withRequiredArg().ofType(Boolean.class).defaultsTo(this.chatSigning);
        this.optionCompressionThreshold = this.optionParser.accepts("compression-threshold").withRequiredArg().ofType(Integer.class).defaultsTo(this.compressionThreshold);
        this.optionAllowBetaPinging = this.optionParser.accepts("allow-beta-pinging").withRequiredArg().ofType(Boolean.class).defaultsTo(this.allowBetaPinging);
        this.optionIgnoreProtocolTranslationErrors = this.optionParser.accepts("ignore-protocol-translation-errors").withRequiredArg().ofType(Boolean.class).defaultsTo(this.ignoreProtocolTranslationErrors);
        this.optionAllowLegacyClientPassthrough = this.optionParser.accepts("allow-legacy-client-passthrough").withRequiredArg().ofType(Boolean.class).defaultsTo(this.allowLegacyClientPassthrough);
        this.optionResourcePackUrl = this.optionParser.accepts("resource-pack-url").withRequiredArg().ofType(String.class).defaultsTo(this.resourcePackUrl);
        this.optionWildcardDomainHandling = this.optionParser.accepts("wildcard-domain-handling").withRequiredArg().ofType(WildcardDomainHandling.class).defaultsTo(this.wildcardDomainHandling);
    }

    @Override
    public void reload() {
        super.reload();

        this.bindAddress = AddressUtil.parse(this.getString("bind-address", AddressUtil.toString(this.bindAddress)), null);
        this.targetVersion = ProtocolVersion.getClosest(this.getString("target-version", this.targetVersion.getName()));
        this.checkTargetVersion();
        this.targetAddress = AddressUtil.parse(this.getString("target-address", AddressUtil.toString(this.targetAddress)), this.targetVersion);
        this.proxyOnlineMode = this.getBoolean("proxy-online-mode", this.proxyOnlineMode);
        this.authMethod = AuthMethod.byName(this.getString("auth-method", this.authMethod.name()));
        final List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
        final int accountIndex = this.getInt("minecraft-account-index", 0);
        if (this.authMethod == AuthMethod.ACCOUNT && accountIndex >= 0 && accountIndex < accounts.size()) {
            this.account = accounts.get(accountIndex);
        } else {
            this.account = null;
        }
        this.betacraftAuth = this.getBoolean("betacraft-auth", this.betacraftAuth);
        this.backendProxyUrl = this.parseProxyUrl(this.getString("backend-proxy-url", ""));
        this.backendHaProxy = this.getBoolean("backend-haproxy", this.backendHaProxy);
        this.chatSigning = this.getBoolean("chat-signing", this.chatSigning);
        this.compressionThreshold = this.getInt("compression-threshold", this.compressionThreshold);
        this.allowBetaPinging = this.getBoolean("allow-beta-pinging", this.allowBetaPinging);
        this.ignoreProtocolTranslationErrors = this.getBoolean("ignore-protocol-translation-errors", this.ignoreProtocolTranslationErrors);
        this.allowLegacyClientPassthrough = this.getBoolean("allow-legacy-client-passthrough", this.allowLegacyClientPassthrough);
        this.resourcePackUrl = this.getString("resource-pack-url", this.resourcePackUrl);
        this.wildcardDomainHandling = WildcardDomainHandling.byName(this.getString("wildcard-domain-handling", this.wildcardDomainHandling.name()));
    }

    public void loadFromArguments(final String[] args) throws IOException {
        try {
            ViaProxy.EVENT_MANAGER.call(new PreOptionsParseEvent(this.optionParser));
            final OptionSet options = this.optionParser.parse(args);
            if (options.has(this.optionHelp)) {
                throw new HelpRequestedException();
            }
            this.bindAddress = AddressUtil.parse(options.valueOf(this.optionBindAddress), null);
            this.targetVersion = options.valueOf(this.optionTargetVersion);
            this.checkTargetVersion();
            this.targetAddress = AddressUtil.parse(options.valueOf(this.optionTargetAddress), this.targetVersion);
            this.proxyOnlineMode = options.valueOf(this.optionProxyOnlineMode);
            this.authMethod = options.valueOf(this.optionAuthMethod);
            final List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
            final int accountIndex = options.valueOf(this.optionMinecraftAccountIndex);
            if (options.has(this.optionMinecraftAccountIndex) && accountIndex >= 0 && accountIndex < accounts.size()) {
                this.authMethod = AuthMethod.ACCOUNT;
                this.account = accounts.get(accountIndex);
            } else {
                this.account = null;
            }
            this.betacraftAuth = options.valueOf(this.optionBetacraftAuth);
            this.backendProxyUrl = this.parseProxyUrl(options.valueOf(this.optionBackendProxyUrl));
            this.backendHaProxy = options.valueOf(this.optionBackendHaProxy);
            this.chatSigning = options.valueOf(this.optionChatSigning);
            this.compressionThreshold = options.valueOf(this.optionCompressionThreshold);
            this.allowBetaPinging = options.valueOf(this.optionAllowBetaPinging);
            this.ignoreProtocolTranslationErrors = options.valueOf(this.optionIgnoreProtocolTranslationErrors);
            this.allowLegacyClientPassthrough = options.valueOf(this.optionAllowLegacyClientPassthrough);
            this.resourcePackUrl = options.valueOf(this.optionResourcePackUrl);
            this.wildcardDomainHandling = options.valueOf(this.optionWildcardDomainHandling);
            ViaProxy.EVENT_MANAGER.call(new PostOptionsParseEvent(options));
            return;
        } catch (OptionException e) {
            Logger.LOGGER.error("Error parsing CLI options: " + e.getMessage());
        } catch (HelpRequestedException ignored) {
        }

        this.optionParser.formatHelpWith(new BetterHelpFormatter());
        this.optionParser.printHelpOn(Logger.SYSOUT);
        Logger.LOGGER.info("For a more detailed description of the options, please refer to the viaproxy.yml file.");
        System.exit(1);
    }

    @Override
    public URL getDefaultConfigURL() {
        return this.getClass().getClassLoader().getResource("assets/viaproxy/viaproxy.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }

    @Override
    public void set(String path, Object value) {
        super.set(path, value);
    }

    public SocketAddress getBindAddress() {
        return this.bindAddress;
    }

    public void setBindAddress(final SocketAddress bindAddress) {
        this.bindAddress = bindAddress;
        this.set("bind-address", AddressUtil.toString(bindAddress));
    }

    public SocketAddress getTargetAddress() {
        return this.targetAddress;
    }

    public void setTargetAddress(final SocketAddress targetAddress) {
        this.targetAddress = targetAddress;
        this.set("target-address", AddressUtil.toString(targetAddress));
    }

    public ProtocolVersion getTargetVersion() {
        return this.targetVersion;
    }

    public void setTargetVersion(final ProtocolVersion targetVersion) {
        this.targetVersion = targetVersion;
        this.set("target-version", targetVersion.getName());
    }

    public boolean isProxyOnlineMode() {
        return this.proxyOnlineMode;
    }

    public void setProxyOnlineMode(final boolean proxyOnlineMode) {
        this.proxyOnlineMode = proxyOnlineMode;
        this.set("proxy-online-mode", proxyOnlineMode);
    }

    public AuthMethod getAuthMethod() {
        return this.authMethod;
    }

    public void setAuthMethod(final AuthMethod authMethod) {
        this.authMethod = authMethod;
        this.set("auth-method", authMethod.name().toLowerCase(Locale.ROOT));
    }

    public Account getAccount() {
        return this.account;
    }

    public void setAccount(final Account account) {
        this.account = account;
        this.set("minecraft-account-index", ViaProxy.getSaveManager().accountsSave.getAccounts().indexOf(account));
    }

    public boolean useBetacraftAuth() {
        return this.betacraftAuth;
    }

    public void setBetacraftAuth(final boolean betacraftAuth) {
        this.betacraftAuth = betacraftAuth;
        this.set("betacraft-auth", betacraftAuth);
    }

    public URI getBackendProxyUrl() {
        return this.backendProxyUrl;
    }

    public void setBackendProxyUrl(final URI backendProxyUrl) {
        this.backendProxyUrl = backendProxyUrl;
        if (backendProxyUrl != null) {
            this.set("backend-proxy-url", backendProxyUrl.toString());
        } else {
            this.set("backend-proxy-url", "");
        }
    }

    public boolean useBackendHaProxy() {
        return this.backendHaProxy;
    }

    public void setBackendHaProxy(final boolean backendHaProxy) {
        this.backendHaProxy = backendHaProxy;
        this.set("backend-haproxy", backendHaProxy);
    }

    public boolean shouldSignChat() {
        return this.chatSigning;
    }

    public void setChatSigning(final boolean chatSigning) {
        this.chatSigning = chatSigning;
        this.set("chat-signing", chatSigning);
    }

    public int getCompressionThreshold() {
        return this.compressionThreshold;
    }

    public void setCompressionThreshold(final int compressionThreshold) {
        this.compressionThreshold = compressionThreshold;
        this.set("compression-threshold", compressionThreshold);
    }

    public boolean shouldAllowBetaPinging() {
        return this.allowBetaPinging;
    }

    public void setAllowBetaPinging(final boolean allowBetaPinging) {
        this.allowBetaPinging = allowBetaPinging;
        this.set("allow-beta-pinging", allowBetaPinging);
    }

    public boolean shouldIgnoreProtocolTranslationErrors() {
        return this.ignoreProtocolTranslationErrors;
    }

    public void setIgnoreProtocolTranslationErrors(final boolean ignoreProtocolTranslationErrors) {
        this.ignoreProtocolTranslationErrors = ignoreProtocolTranslationErrors;
        this.set("ignore-protocol-translation-errors", ignoreProtocolTranslationErrors);
    }

    public boolean shouldAllowLegacyClientPassthrough() {
        return this.allowLegacyClientPassthrough;
    }

    public void setAllowLegacyClientPassthrough(final boolean allowLegacyClientPassthrough) {
        this.allowLegacyClientPassthrough = allowLegacyClientPassthrough;
        this.set("allow-legacy-client-passthrough", allowLegacyClientPassthrough);
    }

    public String getResourcePackUrl() {
        return this.resourcePackUrl;
    }

    public void setResourcePackUrl(final String resourcePackUrl) {
        this.resourcePackUrl = resourcePackUrl;
        this.set("resource-pack-url", resourcePackUrl);
    }

    public WildcardDomainHandling getWildcardDomainHandling() {
        return this.wildcardDomainHandling;
    }

    public void setWildcardDomainHandling(final WildcardDomainHandling wildcardDomainHandling) {
        this.wildcardDomainHandling = wildcardDomainHandling;
        this.set("wildcard-domain-handling", wildcardDomainHandling.name().toLowerCase(Locale.ROOT));
    }

    private void checkTargetVersion() {
        if (this.targetVersion == null) {
            this.targetVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL;
            Logger.LOGGER.info("Invalid target version: " + this.getString("target-version", "") + ". Defaulting to auto detect.");
            Logger.LOGGER.info("=== Supported Protocol Versions ===");
            for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                Logger.LOGGER.info(version.getName());
            }
            Logger.LOGGER.info("===================================");
        }
    }

    private URI parseProxyUrl(final String proxyUrl) {
        if (!proxyUrl.isBlank()) {
            try {
                return new URI(proxyUrl);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid proxy url: " + proxyUrl + ". Proxy url format: type://address:port or type://username:password@address:port");
            }
        }

        return null;
    }

    public enum AuthMethod {

        /**
         * Use an account for joining the target server (Has to be configured in ViaProxy GUI)
         */
        ACCOUNT("tab.general.minecraft_account.option_select_account"),
        /**
         * No authentication (Offline mode)
         */
        NONE("tab.general.minecraft_account.option_no_account"),
        /**
         * Requires the OpenAuthMod client mod (https://modrinth.com/mod/openauthmod)
         */
        OPENAUTHMOD("tab.general.minecraft_account.option_openauthmod");

        private final String guiTranslationKey;

        AuthMethod(String guiTranslationKey) {
            this.guiTranslationKey = guiTranslationKey;
        }

        public static AuthMethod byName(String name) {
            for (AuthMethod mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return NONE;
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
        INTERNAL;

        public static WildcardDomainHandling byName(String name) {
            for (WildcardDomainHandling mode : values()) {
                if (mode.name().equalsIgnoreCase(name)) {
                    return mode;
                }
            }

            return NONE;
        }

    }

}
