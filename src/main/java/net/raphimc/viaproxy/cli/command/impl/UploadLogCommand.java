/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2025 RK_01/RaphiMC and contributors
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
package net.raphimc.viaproxy.cli.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gs.mclo.api.MclogsClient;
import gs.mclo.api.response.UploadLogResponse;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.command.Command;
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor;
import net.raphimc.viaproxy.util.logging.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.ExecutionException;

public class UploadLogCommand extends Command {

    public UploadLogCommand() {
        super("uploadlog", "Upload the ViaProxy log", "uploadlog");
    }

    @Override
    public void register(final LiteralArgumentBuilder<CommandExecutor> builder) {
        builder.executes(context -> {
            final org.apache.logging.log4j.core.Logger logger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            final RollingRandomAccessFileAppender fileAppender = (RollingRandomAccessFileAppender) logger.getAppenders().get("LatestFile");
            fileAppender.getManager().flush();
            final File logFile = new File(fileAppender.getFileName());

            try {
                final MclogsClient mclogsClient = new MclogsClient("ViaProxy", ViaProxy.VERSION);
                final UploadLogResponse apiResponse = mclogsClient.uploadLog(logFile.toPath()).get();
                if (apiResponse.isSuccess()) {
                    context.getSource().sendMessage("Uploaded log file to " + apiResponse.getUrl());
                } else {
                    context.getSource().sendMessage("The log file could not be uploaded: " + apiResponse.getError());
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof FileNotFoundException) {
                    context.getSource().sendMessage("The log file could not be found");
                } else {
                    Logger.LOGGER.error("Failed to upload log file", e.getCause());
                }
            } catch (Throwable e) {
                Logger.LOGGER.error("Failed to upload log file", e);
            }
            return 1;
        });
    }

}
