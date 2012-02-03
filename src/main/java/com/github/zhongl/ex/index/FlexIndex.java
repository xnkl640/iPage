package com.github.zhongl.ex.index;

import com.github.zhongl.ex.codec.Codec;
import com.github.zhongl.ex.lang.Entry;
import com.github.zhongl.ex.page.Cursor;
import com.github.zhongl.ex.page.Number;
import com.github.zhongl.ex.page.Offset;
import com.github.zhongl.ex.page.Page;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
public class FlexIndex extends Index {

    static final int MAX_ENTRY_SIZE = Integer.getInteger("ipage.flex.index.page.max.entry.size", 1024 * 43);

    private static final int CAPACITY = MAX_ENTRY_SIZE * EntryCodec.LENGTH;

    public FlexIndex(File dir) throws IOException { super(dir); }

    @Override
    protected Snapshot newSnapshot(File file, Codec codec) throws IOException {
        return new InnerSnapshot(file, codec);
    }

    private class InnerSnapshot extends Snapshot {

        private Entry<Md5Key, Offset> currentAppendingEntry;

        InnerSnapshot(File dir, Codec codec) throws IOException { super(dir, codec); }

        @Override
        public boolean isEmpty() {
            return pages.get(0).file().length() == 0;
        }

        @Override
        protected Snapshot newSnapshotOn(File dir) throws IOException {
            return new InnerSnapshot(dir, codec);
        }

        @Override
        protected void merge(Iterator<Entry<Md5Key, Offset>> sortedIterator, Snapshot snapshot) throws IOException {
            PeekingIterator<Entry<Md5Key, Offset>> bItr = Iterators.peekingIterator(sortedIterator);

            for (Page page : pages) {
                Partition partition = (Partition) page;
                PeekingIterator<Entry<Md5Key, Offset>> aItr = Iterators.peekingIterator(partition.iterator());
                merge(aItr, bItr, snapshot);
                if (aItr.hasNext()) mergeRestOf(aItr, snapshot);
            }

            if (bItr.hasNext()) mergeRestOf(bItr, snapshot);
        }

        @Override
        public <T> Cursor append(T value, boolean force) throws IOException {
            currentAppendingEntry = (Entry<Md5Key, Offset>) value;
            return super.append(value, force);
        }

        @Override
        protected Number newNumber(@Nullable Page last) {
            return currentAppendingEntry == null ? Md5Key.MIN : currentAppendingEntry.key();
        }

        protected int capacity() {return CAPACITY;}

        @Override
        protected Page newPage(File file, Number number, Codec codec) {
            return new Partition(file, number, capacity(), codec) {
                private int count;

                @Override
                protected boolean checkOverflow(int size, int capacity) {
                    if ((++count) <= FlexIndex.MAX_ENTRY_SIZE) return false;
                    count = 0;
                    return true;
                }
            };
        }
    }
}
