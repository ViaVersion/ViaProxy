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
import net.lenni0451.optconfig.CLIConfigLoader;
import net.lenni0451.optconfig.ConfigContext;
import net.lenni0451.optconfig.ConfigLoader;
import net.lenni0451.optconfig.annotations.*;
import net.lenni0451.optconfig.cli.HelpOptions;
import net.lenni0451.optconfig.cli.UnknownOption;
import net.lenni0451.optconfig.exceptions.CLIIncompatibleOptionException;
import net.lenni0451.optconfig.exceptions.CLIParserException;
import net.lenni0451.optconfig.provider.ConfigProvider;
import net.raphimc.viaproxy.util.logging.Logger;

import java.io.File;
import java.util.List;

@OptConfig
@CheckSuperclasses
public class ViaProxyCLIConfig extends ViaProxyConfig {

    private ConfigContext<ViaProxyCLIConfig> configContext;

    @Hidden
    @Option("help")
    @Description("Displays a help message with a list of all available options")
    private boolean help = false;

    @Hidden
    @Option("extended-help")
    @Description("Displays an extended help message with detailed descriptions of the options")
    private boolean extendedHelp = false;

    @Hidden
    @Option("list-versions")
    @Description("Lists all supported backend server versions and exits")
    private boolean listVersions = false;

    public static ViaProxyCLIConfig create(final File configFile) {
        final ConfigLoader<ViaProxyCLIConfig> configLoader = new ConfigLoader<>(ViaProxyCLIConfig.class);
        configLoader.getConfigOptions().setResetInvalidOptions(true).setRewriteConfig(true).setCommentSpacing(1);
        try {
            return configLoader.load(ConfigProvider.file(configFile)).getConfigInstance();
        } catch (Throwable e) {
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public void loadFromArguments(final String[] args) {
        final CLIConfigLoader<ViaProxyCLIConfig> cliConfigLoader = new CLIConfigLoader<>(this.configContext);
        try {
            final List<UnknownOption> unknownOptions = cliConfigLoader.loadCLIOptions(args, true);
            if (this.help || this.extendedHelp) {
                throw new HelpRequestedException();
            } else if (this.listVersions) {
                Logger.LOGGER.info("=== Supported backend server versions ===");
                for (ProtocolVersion version : ProtocolVersion.getProtocols()) {
                    Logger.LOGGER.info(version.getName());
                }
                Logger.LOGGER.info("===================================");
                System.exit(0);
            }

//            if (options.has("minecraft-account-index")) {
//                this.setAuthMethod(AuthMethod.ACCOUNT);
//            }
            return;
        } catch (CLIParserException e) {
            Logger.LOGGER.fatal("Error parsing CLI options: {}", e.getMessage());
        } catch (CLIIncompatibleOptionException e) {
            Logger.LOGGER.fatal("Incompatible CLI option", e);
            System.exit(1);
        } catch (HelpRequestedException ignored) {
        }

        cliConfigLoader.printCLIHelp(Logger.SYSOUT, HelpOptions.DEFAULT.withShowDescription(this.extendedHelp).withShowDepends(false).withShowBooleanType(true));
        if (!this.extendedHelp) {
            Logger.LOGGER.info("For a more detailed description of the options, use --extended-help or refer to the viaproxy.yml file.");
        }
        System.exit(1);
    }

    @Override
    public void save() {
    }

    private static class HelpRequestedException extends Exception {
    }

}
