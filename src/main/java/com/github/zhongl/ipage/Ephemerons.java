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

package com.github.zhongl.ipage;

import com.github.zhongl.util.Entry;
import com.github.zhongl.util.FutureCallbacks;
import com.github.zhongl.util.Nils;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import org.softee.management.annotation.MBean;
import org.softee.management.annotation.ManagedAttribute;
import org.softee.management.annotation.ManagedOperation;
import org.softee.management.annotation.Parameter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@ThreadSafe
@MBean
public abstract class Ephemerons<V> {

    private final Semaphore flowControl = new Semaphore(0, true);
    private final AtomicLong id = new AtomicLong();
    private final AtomicBoolean flushing = new AtomicBoolean(false);

    private final ConcurrentMap<Key, Record> map;

    protected Ephemerons(ConcurrentMap<Key, Record> map) {this.map = map;}

    @ManagedOperation
    public int throughout(@Parameter("delta") int delta) {
        if (delta > 0) flowControl.release(delta);
        if (delta < 0) flowControl.acquireUninterruptibly(-delta);
        return flowControl.availablePermits();
    }

    @ManagedAttribute
    public boolean isFlushing() {
        return flushing.get();
    }

    @ManagedAttribute
    public int getSize() {
        return map.size();
    }

    public void add(Key key, V value, FutureCallback<Void> removedOrDurableCallback) {
        release(checkNotNull(key), Nils.VOID);
        checkNotNull(value);
        checkNotNull(removedOrDurableCallback);
        acquire();
        map.put(key, new Record(id.getAndIncrement(), key, value, removedOrDurableCallback));
    }

    public void remove(Key key) {
        boolean release = release(checkNotNull(key), Nils.VOID);
        if (release) return;
        acquire();
        map.put(key, new Record(id.getAndIncrement(), key, (V) Nils.OBJECT, FutureCallbacks.<Void>ignore()));
    }

    public V get(Key key) {
        Record record = map.get(key);
        if (record == null) return getMiss(key);
        if (record.value == Nils.OBJECT) return null;
        return record.value;
    }

    public void flush() {
        if (map.isEmpty()) return;
        if (!flushing.compareAndSet(false, true)) return;

        final Collection<Entry<Key, V>> appendings = new ArrayList<Entry<Key, V>>();
        final Collection<Key> removings = new ArrayList<Key>();

        SortedSet<Record> records = new TreeSet<Record>(map.values());

        for (Record record : records) {
            if (record.value == Nils.OBJECT) removings.add(record.key);
            else appendings.add(new Entry<Key, V>(record.key, record.value));
        }

        requestFlush(appendings, removings, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                foreachKey(new Function<Key, Void>() {
                    @Override
                    public Void apply(@Nullable Key key) {
                        release(key, Nils.VOID);
                        return Nils.VOID;
                    }

                });
            }

            @Override
            public void onFailure(final Throwable t) {
                foreachKey(new Function<Key, Void>() {
                    @Override
                    public Void apply(@Nullable Key key) {
                        release(key, t);
                        return Nils.VOID;
                    }
                });
            }

            private void foreachKey(Function<Key, Void> function) {
                for (Key key : removings) function.apply(key);
                for (Entry<Key, V> entry : appendings) function.apply(entry.key());
                flushing.set(false);
            }
        });
    }

    /** This method supposed be asynchronized. */
    protected abstract void requestFlush(Collection<Entry<Key, V>> appendings, Collection<Key> removings, FutureCallback<Void> flushedCallback);

    /** This method supposed be thread safed. */
    protected abstract V getMiss(Key key);

    private void acquire() {
        try {
            while (!flowControl.tryAcquire(500L, TimeUnit.MILLISECONDS)) flush();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean release(Key key, Object voidOrThrowable) {
        Record record = map.remove(key);

        if (record == null) return false;

        flowControl.release();
        if (voidOrThrowable == Nils.VOID)
            record.callback.onSuccess(Nils.VOID);
        else
            record.callback.onFailure((Throwable) voidOrThrowable);
        return true;
    }

    protected class Record implements Comparable<Record> {

        private final Long id;
        private final Key key;
        private final V value;
        private final FutureCallback<Void> callback;

        public Record(long id, Key key, V value, FutureCallback<Void> callback) {
            this.id = id;
            this.key = key;
            this.value = value;
            this.callback = callback;
        }

        @Override
        public int compareTo(Record o) {
            return id.compareTo(o.id);
        }
    }
}