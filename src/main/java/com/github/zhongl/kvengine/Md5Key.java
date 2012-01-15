/*
 * Copyright 2011 zhongl
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.zhongl.kvengine;

import com.github.zhongl.nio.AbstractAccessor;
import com.github.zhongl.nio.Accessor;

import javax.annotation.concurrent.ThreadSafe;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@ThreadSafe
public class Md5Key {

    public static final int BYTE_LENGTH = 16;
    public static final Accessor<Md5Key> ACCESSOR = new InnerAccessor();

    private final byte[] md5Bytes;

    public static byte[] md5(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Md5Key generate(byte[] bytes) {
        return new Md5Key(md5(bytes));
    }

    public Md5Key(byte[] md5Bytes) {
        checkArgument(md5Bytes.length == BYTE_LENGTH, "Invalid generate bytes length %s", md5Bytes.length);
        this.md5Bytes = md5Bytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Md5Key md5Key = (Md5Key) o;
        return Arrays.equals(md5Bytes, md5Key.md5Bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(md5Bytes);
    }

    public byte[] toBytes() {
        return md5Bytes;
    }

    private static class InnerAccessor extends AbstractAccessor<Md5Key> {

        @Override
        public int byteLengthOf(Md5Key object) {
            return BYTE_LENGTH;
        }

        @Override
        public Md5Key read(ByteBuffer buffer) {
            byte[] bytes = new byte[BYTE_LENGTH];
            buffer.get(bytes);
            return new Md5Key(bytes);
        }

        @Override
        protected void doWrite(Md5Key object, ByteBuffer buffer) {
            buffer.put(object.md5Bytes);
        }
    }
}