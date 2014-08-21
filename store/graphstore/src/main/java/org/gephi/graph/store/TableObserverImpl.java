/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.graph.store;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Collections;
import java.util.List;
import org.gephi.attribute.api.Column;
import org.gephi.attribute.api.Table;
import org.gephi.attribute.api.TableDiff;
import org.gephi.attribute.api.TableObserver;

/**
 *
 * @author mbastian
 */
public class TableObserverImpl implements TableObserver {

    protected final TableImpl table;
    protected boolean destroyed;
    //Config
    protected final boolean withDiff;
    //Hashcodes
    protected int tableHash;
    //Cache
    protected TableDiffImpl tableDiff;
    protected Column[] columnCache;

    public TableObserverImpl(TableImpl table) {
        this(table, false);
    }

    public TableObserverImpl(TableImpl table, boolean withDiff) {
        this.table = table;
        this.withDiff = withDiff;
        this.columnCache = table.getColumns();
        
        tableHash = table.hashCode();
    }

    @Override
    public synchronized boolean hasTableChanged() {
        int newHash = table.hashCode();
        boolean changed = newHash != tableHash;
        tableHash = newHash;
        if (changed && withDiff) {
            refreshDiff();
        }
        return changed;
    }
    
    @Override
    public synchronized TableDiff getDiff() {
        if (!withDiff) {
            throw new RuntimeException("This observer doesn't compute diffs, set diff setting to true");
        }
        if (tableDiff == null) {
            throw new IllegalStateException("The hasGraphChanged() method should be called first and getDiff() only once then");
        }
        TableDiff diff = tableDiff;
        tableDiff = null;
        return diff;
    }

    @Override
    public Table getTable() {
        return table;
    }

    @Override
    public void destroy() {
        table.destroyTableObserver(this);
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    protected void destroyObserver() {
        tableHash = 0;
        columnCache = null;
        tableDiff = null;
        destroyed = true;
    }
    
        
    protected void refreshDiff() {
        Column[] currentColumns = table.getColumns();
        tableDiff = new TableDiffImpl(currentColumns, columnCache);
        columnCache = currentColumns;
    }

    protected static final class TableDiffImpl implements TableDiff {

        protected final ObjectList<Column> addedColumns;
        protected final ObjectList<Column> removedColumns;
        
        public TableDiffImpl(Column[] currentColumns, Column[] columnCache) {
            addedColumns = new ObjectArrayList<Column>();
            removedColumns = new ObjectArrayList<Column>();
        
            for (Column currentColumn : currentColumns) {
                if (!isContained(currentColumn, columnCache)) {
                    addedColumns.add(currentColumn);
                }
            }
        
            for (Column cachedColumn : columnCache) {
                if (!isContained(cachedColumn, currentColumns)) {
                    removedColumns.add(cachedColumn);
                }
            }
        }

        private boolean isContained(Column searchedColumn, Column[] columns) {
            for (Column column : columns) {
                if (searchedColumn.equals(column)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public List<Column> getAddedColumns() {
            if (!addedColumns.isEmpty()) {
                return Collections.unmodifiableList(addedColumns);
            }
            return Collections.emptyList();
        }

        @Override
        public List<Column> getRemovedColumns() {
            if (!removedColumns.isEmpty()) {
                 return Collections.unmodifiableList(removedColumns);
            }
            return Collections.emptyList();
        }
    }
}
