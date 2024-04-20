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
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.options.Options;
import net.raphimc.viaproxy.protocoltranslator.ProtocolTranslator;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.util.AddressUtil;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ViaProxyConfig extends Config implements com.viaversion.viaversion.api.configuration.Config {

    private SocketAddress bindAddress;
    private SocketAddress targetAddress;
    private ProtocolVersion targetVersion;
    private boolean proxyOnlineMode;
    private AuthMethod authMethod;
    private Account account;
    private boolean betacraftAuth;
    private URI backendProxyUrl;
    private boolean backendHaProxy;
    private boolean chatSigning;
    private int compressionThreshold;
    private boolean allowBetaPinging;
    private boolean ignoreProtocolTranslationErrors;
    private boolean allowLegacyClientPassthrough;
    private String resourcePackUrl;
    private WildcardDomainHandling wildcardDomainHandling;

    public ViaProxyConfig(final File configFile) {
        super(configFile);
    }

    @Override
    public void reload() {
        super.reload();
        this.loadFields();
    }

    private void loadFields() {
        this.bindAddress = AddressUtil.parse(this.getString("bind-address", "0.0.0.0:25568"), null);
        this.targetVersion = ProtocolVersion.getClosest(this.getString("target-version", ProtocolTranslator.AUTO_DETECT_PROTOCOL.getName()));
        if (this.targetVersion == null) {
            this.targetVersion = ProtocolTranslator.AUTO_DETECT_PROTOCOL;
            Logger.LOGGER.info("Invalid target version: " + this.getString("target-version", "") + ". Defaulting to auto detect.");
            Logger.LOGGER.info("=== Supported Protocol Versions ===");
            for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                Logger.LOGGER.info(version.getName());
            }
            Logger.LOGGER.info("===================================");
        }
        this.targetAddress = AddressUtil.parse(this.getString("target-address", "127.0.0.1:25565"), this.targetVersion);
        this.proxyOnlineMode = this.getBoolean("proxy-online-mode", false);
        this.authMethod = AuthMethod.byName(this.getString("auth-method", "none"));
        final List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
        final int accountIndex = this.getInt("minecraft-account-index", 0);
        if (this.authMethod == AuthMethod.ACCOUNT && accountIndex >= 0 && accountIndex < accounts.size()) {
            this.account = accounts.get(accountIndex);
        } else {
            this.account = null;
        }
        this.betacraftAuth = this.getBoolean("betacraft-auth", false);
        final String proxyUrl = this.getString("backend-proxy-url", "");
        if (!proxyUrl.isBlank()) {
            try {
                this.backendProxyUrl = new URI(proxyUrl);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid proxy url: " + proxyUrl + ". Proxy url format: type://address:port or type://username:password@address:port");
            }
        }
        this.backendHaProxy = this.getBoolean("backend-haproxy", false);
        this.chatSigning = this.getBoolean("chat-signing", true);
        this.compressionThreshold = this.getInt("compression-threshold", 256);
        this.allowBetaPinging = this.getBoolean("allow-beta-pinging", false);
        this.ignoreProtocolTranslationErrors = this.getBoolean("ignore-protocol-translation-errors", false);
        this.allowLegacyClientPassthrough = this.getBoolean("allow-legacy-client-passthrough", false);
        this.resourcePackUrl = this.getString("resource-pack-url", "");
        this.wildcardDomainHandling = WildcardDomainHandling.byName(this.getString("wildcard-domain-handling", "none"));
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
        Options.loadFromConfig(this);
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
