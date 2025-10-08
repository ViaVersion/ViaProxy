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
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.cli.command.Command;
import net.raphimc.viaproxy.cli.command.executor.CommandExecutor;
import net.raphimc.viaproxy.saves.impl.accounts.Account;
import net.raphimc.viaproxy.saves.impl.accounts.BedrockAccount;
import net.raphimc.viaproxy.saves.impl.accounts.MicrosoftAccount;
import net.raphimc.viaproxy.util.TFunction;

import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class AccountCommand extends Command {

    public AccountCommand() {
        super("account", "Manage the account list and the currently selected account", "account list/select <index>/deselect/add (offline <name>/microsoft/bedrock)/remove <index>");
    }

    @Override
    public void register(LiteralArgumentBuilder<CommandExecutor> builder) {
        builder.then(literal("list").executes(ctx -> {
            List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
            if (accounts.isEmpty()) {
                ctx.getSource().sendMessage("No accounts added yet.");
            } else {
                for (int i = 0; i < accounts.size(); i++) {
                    boolean isSelected = ViaProxy.getConfig().getAccount() == accounts.get(i);
                    ctx.getSource().sendMessage("[" + i + "] " + accounts.get(i).getDisplayString() + (isSelected ? " <--" : ""));
                }
            }
            return 1;
        }));
        builder.then(literal("deselect").executes(ctx -> {
            ViaProxy.getConfig().setAccount(null);
            ctx.getSource().sendMessage("Deselected current account.");
            return 1;
        }));
        builder.then(literal("select").then(argument("index", integer(0)).executes(ctx -> {
            List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
            if (accounts.isEmpty()) {
                ctx.getSource().sendMessage("No accounts to select from.");
                return 0;
            }
            int index = getInteger(ctx, "index");
            if (index < 0 || index >= accounts.size()) {
                ctx.getSource().sendMessage("Invalid account index (run 'account list' to see a list of all accounts).");
                return 0;
            }
            Account account = ViaProxy.getSaveManager().accountsSave.getAccounts().get(index);
            ViaProxy.getConfig().setAccount(account);
            ctx.getSource().sendMessage("Selected account " + index + ": " + account.getDisplayString() + ".");
            return 1;
        })));
        builder.then(literal("add")
                .then(literal("offline").then(argument("name", string()).executes(ctx -> {
                    String name = getString(ctx, "name");
                    ViaProxy.getSaveManager().accountsSave.addAccount(name);
                    ViaProxy.getSaveManager().save();
                    ctx.getSource().sendMessage("Added offline account '" + name + "'.");
                    return 1;
                })))
                .then(literal("microsoft").executes(ctx -> {
                    return this.handleLogin(ctx.getSource(), codeConsumer -> {
                        return new MicrosoftAccount(MicrosoftAccount.DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(codeConsumer)));
                    });
                }))
                .then(literal("bedrock").executes(ctx -> {
                    return this.handleLogin(ctx.getSource(), codeConsumer -> {
                        return new BedrockAccount(BedrockAccount.DEVICE_CODE_LOGIN.getFromInput(MinecraftAuth.createHttpClient(), new StepMsaDeviceCode.MsaDeviceCodeCallback(codeConsumer)));
                    });
                }))
        );
        builder.then(literal("remove").then(argument("index", integer(0)).executes(ctx -> {
            List<Account> accounts = ViaProxy.getSaveManager().accountsSave.getAccounts();
            if (accounts.isEmpty()) {
                ctx.getSource().sendMessage("No accounts to remove.");
                return 0;
            }
            int index = getInteger(ctx, "index");
            if (index < 0 || index >= accounts.size()) {
                ctx.getSource().sendMessage("Invalid account index (run 'account list' to see a list of all accounts).");
                return 0;
            }
            Account account = ViaProxy.getSaveManager().accountsSave.getAccounts().get(index);
            ViaProxy.getSaveManager().accountsSave.removeAccount(account);
            ViaProxy.getSaveManager().save();
            if (ViaProxy.getConfig().getAccount() == account) {
                ViaProxy.getConfig().setAccount(null);
            }
            ctx.getSource().sendMessage("Removed account " + index + ": " + account.getDisplayString() + ".");
            return 1;
        })));
    }

    private int handleLogin(final CommandExecutor source, final TFunction<Consumer<StepMsaDeviceCode.MsaDeviceCode>, Account> requestHandler) {
        try {
            Account account = requestHandler.apply(code -> {
                source.sendMessage("Please open your browser and visit " + code.getDirectVerificationUri() + " and login with your Microsoft account.");
                source.sendMessage("If the code is not inserted automatically, please enter the code: " + code.getUserCode() + ".");
            });
            ViaProxy.getSaveManager().accountsSave.addAccount(account);
            ViaProxy.getSaveManager().save();
            return 1;
        } catch (InterruptedException ignored) {
        } catch (TimeoutException e) {
            source.sendMessage("The authentication process timed out.");
        } catch (Throwable t) {
            source.sendMessage("An error occurred while trying to authenticate: " + t.getMessage());
            t.printStackTrace();
        }
        return 0;
    }

}
