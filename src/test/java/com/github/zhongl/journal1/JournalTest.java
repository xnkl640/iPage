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

package com.github.zhongl.journal1;

import com.github.zhongl.util.FileBase;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class JournalTest extends FileBase {

    @Test
    public void normal() throws Exception {
        dir = testDir("normal");

        EventLoader loader = null;

        Journal journal = Journal.open(dir, loader);

        Event event = new MockEvent();

        assertThat(journal.headEvent(), is(nullValue()));

        journal.append(event);

        assertThat(journal.headEvent(), is(event));

        journal.removeHeadEvent();

        assertThat(journal.headEvent(), is(nullValue()));

        journal.close();
    }

    static class MockEvent implements Event {

    }

    /*

    Event -> Entity

    Handler

    committer.commit(event)

    Journal journal = new Journal(committer)



    journal.append(event, callback)

    journal.append(event) {
        try{
            event.writeTo(channel)
            event.recorded()
        } catch(Throwable t){
            event.recordFailedBecauseOf(t)
        }

    }

    Event event = journal.headEvent();

    journal.removeHeadEvent()

    case Append_Event:
    case Delete_Event:


    */

}
