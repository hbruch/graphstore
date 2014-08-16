/*
 * Copyright 2012-2013 Gephi Consortium
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gephi.graph.store;

import org.gephi.graph.api.Node;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author mbastian
 */
public class TableObserverTest {

    @Test
    public void testDefaultObserver() {
        TableImpl table = new TableImpl(new ColumnStore(Node.class, false));
        TableObserverImpl tableObserver = (TableObserverImpl) table.newTableObserver();

        Assert.assertFalse(tableObserver.destroyed);
        Assert.assertEquals(table.hashCode(), tableObserver.tableHash);
        Assert.assertTrue(table.store.observers.contains(tableObserver));

        Assert.assertFalse(tableObserver.hasTableChanged());
    }

    @Test
    public void testObserverAddColumn() {
        TableImpl table = new TableImpl(new ColumnStore(Node.class, false));
        TableObserverImpl tableObserver = (TableObserverImpl) table.newTableObserver();

        table.addColumn("0", Integer.class);

        Assert.assertTrue(tableObserver.hasTableChanged());
        Assert.assertFalse(tableObserver.hasTableChanged());
    }

    @Test
    public void testDestroyObserver() {
        TableImpl table = new TableImpl(new ColumnStore(Node.class, false));
        TableObserverImpl tableObserver = (TableObserverImpl) table.newTableObserver();

        tableObserver.destroy();

        Assert.assertTrue(tableObserver.destroyed);
        Assert.assertFalse(table.store.observers.contains(tableObserver));
    }
}
