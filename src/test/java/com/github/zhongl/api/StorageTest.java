/*
 * Copyright 2012 zhongl
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

package com.github.zhongl.api;

import com.github.zhongl.codec.StringCodec;
import com.github.zhongl.index.Key;
import com.github.zhongl.util.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class StorageTest extends FileTestContext {

    @Test
    public void addAndRemove() throws Exception {
        dir = testDir("addAndRemove");

        final String value = "value";
        final Key key = key(value);

        final Storage<String> storage = new Storage<String>(dir, new StringCodec());

        CallbackFuture<Void> callback;
        callback = new CallbackFuture<Void>();
        storage.merge(Arrays.asList(new Entry<Key, String>(key, value)), Collections.<Key>emptyList(), callback);
        FutureCallbacks.getUnchecked(callback);

        assertThat(storage.get(key), is(value));
        assertThat(storage.getTotal(), is(1));

        callback = new CallbackFuture<Void>();
        storage.merge(Collections.<Entry<Key, String>>emptyList(), Arrays.asList(key), callback);
        FutureCallbacks.getUnchecked(callback);

        assertThat(storage.get(key), is(nullValue()));

        // issue #31 : Negative alives
        storage.merge(Collections.<Entry<Key, String>>emptyList(), Arrays.asList(key, key), callback);
        FutureCallbacks.getUnchecked(callback);
        assertThat(storage.getTotal(), is(0));
    }

    @Test
    public void getNonexistKey() throws Exception {
        dir = testDir("getNonexistKey");

        final String value = "value";

        final Storage<String> storage = new Storage<String>(dir, new StringCodec());

        CallbackFuture<Void> callback = new CallbackFuture<Void>();
        storage.merge(Arrays.asList(new Entry<Key, String>(key(value), value)), Collections.<Key>emptyList(), callback);
        FutureCallbacks.getUnchecked(callback);

        assertThat(storage.get(key("nonexist")), is(nullValue()));
    }

    @Test
    public void iterate() throws Exception {
        dir = testDir("iterate");

        final Storage<String> storage = new Storage<String>(dir, new StringCodec());

        final List<Entry<Key, String>> appendings = Arrays.asList(
                new Entry<Key, String>(key("1"), "1"),
                new Entry<Key, String>(key("2"), "2"),
                new Entry<Key, String>(key("3"), "3")
        );

        CallbackFuture<Void> callback;

        callback = new CallbackFuture<Void>();
        storage.merge(appendings, Collections.<Key>emptyList(), callback);
        FutureCallbacks.getUnchecked(callback);

        assertIterate(storage, "1", "2", "3");

        callback = new CallbackFuture<Void>();
        storage.merge(Collections.<Entry<Key, String>>emptyList(), Arrays.asList(key("2")), callback);
        FutureCallbacks.getUnchecked(callback);

        assertIterate(storage, "1", "3");

        callback = new CallbackFuture<Void>();
        storage.merge(Collections.<Entry<Key, String>>emptyList(), Arrays.asList(key("1")), callback);
        FutureCallbacks.getUnchecked(callback);

        assertIterate(storage, "3");
    }

    @Test
    public void issue36() throws Exception {
        // Fixed #36 : Appendings should not be removed during defragment.

        dir = testDir("issue36");

        final Storage<String> storage = new Storage<String>(dir, new StringCodec());

        List<Entry<Key, String>> appendings;
        appendings = Arrays.asList(
                new Entry<Key, String>(key("1"), "1"),
                new Entry<Key, String>(key("2"), "2"),
                new Entry<Key, String>(key("3"), "3")
        );

        CallbackFuture<Void> callback;

        callback = new CallbackFuture<Void>();
        storage.merge(appendings, Collections.<Key>emptyList(), callback);
        FutureCallbacks.getUnchecked(callback);

        appendings = Arrays.asList(
                new Entry<Key, String>(key("4"), "4"),
                new Entry<Key, String>(key("5"), "5")
        );

        List<Key> removings = Arrays.asList(key("1"), key("2"), key("3"));

        callback = new CallbackFuture<Void>();
        storage.merge(appendings, removings, callback);
        FutureCallbacks.getUnchecked(callback);

        assertThat(storage.get(key("4")), is("4"));
        assertThat(storage.get(key("5")), is("5"));
    }

    private <T> void assertIterate(Iterable<T> iterable, T... values) {
        Iterator<T> iterator = iterable.iterator();
        for (T value : values) {
            assertThat(iterator.hasNext(), is(true));
            assertThat(iterator.next(), is(value));
        }
    }

    private Key key(String s) {return new Key(Md5.md5(s.getBytes()));}

}