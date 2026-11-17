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
package net.raphimc.viaproxy.proxy.util;

import net.raphimc.netminecraft.netty.crypto.AESEncryption;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * Encryption with separate ciphers per direction, as used since 26.4.
 */
public class SplitAESEncryption extends AESEncryption {

    private final Cipher inCipher;
    private final Cipher outCipher;

    public SplitAESEncryption(final Cipher inCipher, final Cipher outCipher) throws GeneralSecurityException {
        super(new SecretKeySpec(new byte[16], "AES")); // Unused
        this.inCipher = inCipher;
        this.outCipher = outCipher;
    }

    @Override
    public int getDecryptOutputSize(final int length) {
        return this.inCipher.getOutputSize(length);
    }

    @Override
    public int getEncryptOutputSize(final int length) {
        return this.outCipher.getOutputSize(length);
    }

    @Override
    public int decrypt(final byte[] input, final int inputOffset, final int inputLength, final byte[] output, final int outputOffset) throws Exception {
        return this.inCipher.update(input, inputOffset, inputLength, output, outputOffset);
    }

    @Override
    public int encrypt(final byte[] input, final int inputOffset, final int inputLength, final byte[] output, final int outputOffset) throws Exception {
        return this.outCipher.update(input, inputOffset, inputLength, output, outputOffset);
    }

}
