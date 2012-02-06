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

package com.github.zhongl.ex.page;

import com.github.zhongl.ex.nio.ByteBuffers;
import com.github.zhongl.ex.nio.Forcer;
import com.github.zhongl.ex.util.Tuple;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.Queue;

import static com.github.zhongl.ex.nio.ByteBuffers.lengthOf;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
public class DefaultBatch extends Batch {
    protected final int start;
    protected final int estimateBufferSize;
    protected final Queue<Runnable> delayTransformQueue;

    private final Queue<Tuple> tupleQueue;
    private final CursorFactory cursorFactory;

    public DefaultBatch(CursorFactory cursorFactory, int start, int estimateBufferSize) {
        this.cursorFactory = cursorFactory;
        this.start = start;
        this.estimateBufferSize = Math.max(4096, estimateBufferSize);
        this.delayTransformQueue = new LinkedList<Runnable>();
        this.tupleQueue = new LinkedList<Tuple>();
    }

    @Override
    protected final Cursor _append(Object object) {
        ObjectRef objectRef = cursorFactory.objectRef(object);
        Proxy proxy = cursorFactory.proxy(objectRef);
        onAppend(objectRef, proxy);
        return proxy;
    }

    protected void onAppend(final ObjectRef objectRef, final Proxy proxy) {
        tupleQueue.offer(new Tuple(proxy, objectRef.encode()));
    }

    @Override
    protected final int _writeAndForceTo(FileChannel channel) throws IOException {
        int size = Forcer.getInstance().force(channel, aggregate());
        while (!delayTransformQueue.isEmpty())
            delayTransformQueue.poll().run();
        return size;
    }

    private ByteBuffer aggregate() throws IOException {
        int position = start;
        ByteBuffer aggregated = ByteBuffer.allocate(estimateBufferSize);

        for (Tuple tuple : toAggregatingQueue()) {
            final Proxy proxy = tuple.get(0);
            final ByteBuffer buffer = tuple.get(1);

            final int offset = position;
            delayTransformQueue.offer(new Runnable() {
                @Override
                public void run() {
                    proxy.transform(cursorFactory.reader(offset));
                }
            });

            position = position + lengthOf(buffer);
            aggregated = ByteBuffers.aggregate(aggregated, buffer);
        }

        return (ByteBuffer) aggregated.flip();
    }

    protected Iterable<Tuple> toAggregatingQueue() { return tupleQueue; }
}
