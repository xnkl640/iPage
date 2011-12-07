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

package com.github.zhongl.ipage;

import java.nio.ByteBuffer;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public interface ByteBufferAccessor<T> {
    /**
     * Byte length of the object store in {@link java.nio.ByteBuffer}.
     *
     * @param object T
     *
     * @return length
     */
    int lengthOf(T object);

    Writer write(T object);

    Reader<T> read(ByteBuffer buffer);

    interface Reader<T> {
        /**
         * Get object from {@link java.nio.ByteBuffer}.
         *
         * @return instance of T
         */
        public T get();
    }

    interface Writer {
        /**
         * Write object to {@link java.nio.ByteBuffer}.
         *
         * @param buffer {@link java.nio.ByteBuffer}
         *
         * @return wrote length.
         */
        public int to(ByteBuffer buffer);
    }
}
