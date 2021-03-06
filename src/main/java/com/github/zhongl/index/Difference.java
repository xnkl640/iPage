/*
 * Copyright 2012 zhongl
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

package com.github.zhongl.index;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
@NotThreadSafe
public class Difference implements Iterable<Index> {

    private final SortedSet<Index> set;

    public Difference(SortedSet<Index> set) {this.set = set;}

    public void addAll(Collection<? extends Index> c) {
        for (Index index : c) add(index);
    }

    public void add(Index index) {
        set.remove(index);
        set.add(index);
    }

    @Override
    public Iterator<Index> iterator() {
        return set.iterator();
    }
}
