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

import it.unimi.dsi.fastutil.bytes.Byte2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.doubles.Double2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.floats.Float2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.shorts.Short2ObjectAVLTreeMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.gephi.attribute.api.Column;
import org.gephi.attribute.api.Index;
import org.gephi.graph.api.Element;

/**
 *
 * @author mbastian
 */
public class IndexImpl<T extends Element> implements Index<T> {

    protected final TableLock lock;
    protected final ColumnStore<T> columnStore;
    protected AbstractIndex[] columns;

    public IndexImpl(ColumnStore<T> columnStore) {
        this.columnStore = columnStore;
        this.columns = new AbstractIndex[0];
        this.lock = columnStore.lock;
    }

    @Override
    public Class<T> getIndexClass() {
        return columnStore.elementType;
    }

    @Override
    public String getIndexName() {
        return "index_" + columnStore.elementType.getCanonicalName();
    }

    @Override
    public int count(Column column, Object value) {
        checkNonNullColumnObject(column);

        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return index.getCount(value);
        } finally {
            unlock();
        }
    }

    public int count(String key, Object value) {
        checkNonNullObject(key);

        AbstractIndex index = getIndex(key);
        return index.getCount(value);
    }

    public Iterable<T> get(String key, Object value) {
        checkNonNullObject(key);

        AbstractIndex index = getIndex(key);
        return index.getValueSet(value);
    }

    @Override
    public Iterable<T> get(Column column, Object value) {
        checkNonNullColumnObject(column);

        if (lock != null) {
            lock.lock();
            AbstractIndex index = getIndex((ColumnImpl) column);
            return new LockableIterable<T>(index.getValueSet(value));
        }
        AbstractIndex index = getIndex((ColumnImpl) column);
        return index.getValueSet(value);
    }

    @Override
    public Number getMinValue(Column column) {
        checkNonNullColumnObject(column);

        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return index.getMinValue();
        } finally {
            unlock();
        }
    }

    @Override
    public Number getMaxValue(Column column) {
        checkNonNullColumnObject(column);
        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return index.getMaxValue();
        } finally {
            unlock();
        }
    }

    public Iterable<Map.Entry<Object, Set<T>>> get(Column column) {
        checkNonNullColumnObject(column);

        AbstractIndex index = getIndex((ColumnImpl) column);
        return index;
    }

    @Override
    public Collection values(Column column) {
        checkNonNullColumnObject(column);

        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return new ArrayList(index.values());
        } finally {
            unlock();
        }
    }

    @Override
    public int countValues(Column column) {
        checkNonNullColumnObject(column);
        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return index.countValues();
        } finally {
            unlock();
        }
    }

    @Override
    public int countElements(Column column) {
        checkNonNullColumnObject(column);
        lock();
        try {
            AbstractIndex index = getIndex((ColumnImpl) column);
            return index.elements;
        } finally {
            unlock();
        }
    }

    public Object put(String key, Object value, T element) {
        checkNonNullObject(key);

        AbstractIndex index = getIndex(key);
        return index.putValue(element, value);
    }

    public Object put(Column column, Object value, T element) {
        checkNonNullColumnObject(column);

        AbstractIndex index = getIndex((ColumnImpl) column);
        return index.putValue(element, value);
    }

    public void remove(String key, Object value, T element) {
        checkNonNullObject(key);

        AbstractIndex index = getIndex(key);
        index.removeValue(element, value);
    }

    public void remove(Column column, Object value, T element) {
        checkNonNullColumnObject(column);

        AbstractIndex index = getIndex((ColumnImpl) column);
        index.removeValue(element, value);
    }

    public Object set(String key, Object oldValue, Object value, T element) {
        checkNonNullObject(key);

        AbstractIndex index = getIndex(key);
        return index.replaceValue(element, oldValue, value);
    }

    public Object set(Column column, Object oldValue, Object value, T element) {
        checkNonNullColumnObject(column);

        AbstractIndex index = getIndex((ColumnImpl) column);
        return index.replaceValue(element, oldValue, value);
    }

    public void clear() {
        for (AbstractIndex ai : columns) {
            if (ai != null) {
                ai.clear();
            }
        }
    }

    protected void addColumn(ColumnImpl col) {
        if (col.isIndexed()) {
            ensureColumnSize(col.storeId);
            AbstractIndex index = createIndex(col);
            columns[col.storeId] = index;
        }
    }

    protected void addAllColumns(ColumnImpl[] cols) {
        ensureColumnSize(cols.length);
        for (ColumnImpl col : cols) {
            if (col.isIndexed()) {
                AbstractIndex index = createIndex(col);
                columns[col.storeId] = index;
            }
        }
    }

    protected void removeColumn(ColumnImpl col) {
        if (col.isIndexed()) {
            AbstractIndex index = columns[col.storeId];
            index.destroy();
            columns[col.storeId] = null;
        }
    }

    protected boolean hasColumn(ColumnImpl col) {
        if (col.isIndexed()) {
            int id = col.storeId;
            if (id != ColumnStore.NULL_ID && columns[id].column == col) {
                return true;
            }
        }
        return false;
    }

    protected AbstractIndex getIndex(ColumnImpl column) {
        return columns[column.getStoreId()];
    }

    protected AbstractIndex getIndex(String key) {
        return columns[columnStore.getColumnIndex(key)];
    }

    protected void destroy() {
        for (AbstractIndex ai : columns) {
            ai.destroy();
        }
        columns = null;
    }
    
    protected int size() {
        return columns.length;
    }

    AbstractIndex createIndex(ColumnImpl column) {
        if (column.getTypeClass().equals(Byte.class)) {
            //Byte
            return new ByteIndex(column);
        } else if (column.getTypeClass().equals(Short.class)) {
            //Short
            return new ShortIndex(column);
        } else if (column.getTypeClass().equals(Integer.class)) {
            //Integer
            return new IntegerIndex(column);
        } else if (column.getTypeClass().equals(Long.class)) {
            //Long
            return new LongIndex(column);
        } else if (column.getTypeClass().equals(Float.class)) {
            //Float
            return new FloatIndex(column);
        } else if (column.getTypeClass().equals(Double.class)) {
            //Double
            return new DoubleIndex(column);
        } else if (column.getTypeClass().equals(Boolean.class)) {
            //Boolean
            return new BooleanIndex(column);
        } else if (column.getTypeClass().equals(Character.class)) {
            //Char
            return new CharIndex(column);
        } else if (column.getTypeClass().equals(String.class)) {
            //String
            return new DefaultIndex(column);
        } else if (column.getTypeClass().equals(byte[].class)) {
            //Byte Array
            return new ByteArrayIndex(column);
        } else if (column.getTypeClass().equals(short[].class)) {
            //Short Array
            return new ShortArrayIndex(column);
        } else if (column.getTypeClass().equals(int[].class)) {
            //Integer Array
            return new IntegerArrayIndex(column);
        } else if (column.getTypeClass().equals(long[].class)) {
            //Long Array
            return new LongArrayIndex(column);
        } else if (column.getTypeClass().equals(float[].class)) {
            //Float array
            return new FloatArrayIndex(column);
        } else if (column.getTypeClass().equals(double[].class)) {
            //Double array
            return new DoubleArrayIndex(column);
        } else if (column.getTypeClass().equals(boolean[].class)) {
            //Boolean array
            return new BooleanArrayIndex(column);
        } else if (column.getTypeClass().equals(char[].class)) {
            //Char array
            return new CharArrayIndex(column);
        } else if (column.getTypeClass().equals(String[].class)) {
            //String array
            return new DefaultArrayIndex(column);
        } else if (column.getTypeClass().isArray()) {
            //Default Array
            return new DefaultArrayIndex(column);
        }
        return new DefaultIndex(column);
    }

    private void ensureColumnSize(int index) {
        if (index >= columns.length) {
            AbstractIndex[] newArray = new AbstractIndex[index + 1];
            System.arraycopy(columns, 0, newArray, 0, columns.length);
            columns = newArray;
        }
    }

    void lock() {
        if (lock != null) {
            lock.lock();
        }
    }

    void unlock() {
        if (lock != null) {
            lock.unlock();
        }
    }

    void checkNonNullObject(final Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
    }

    void checkNonNullColumnObject(final Object o) {
        if (o == null) {
            throw new NullPointerException();
        }
        if (!(o instanceof ColumnImpl)) {
            throw new ClassCastException("Must be ColumnImpl object");
        }
    }

    protected abstract class AbstractIndex<K> implements Iterable<Map.Entry<K, Set<T>>> {

        //Const
        public static final boolean TRIMMING_ENABLED = false;
        public static final int TRIMMING_FREQUENCY = 30;
        //Data
        protected final ColumnImpl column;
        protected final Set<T> nullSet;
        protected Map<K, Set<T>> map;
        //Variable
        protected int elements;

        public AbstractIndex(ColumnImpl column) {
            this.column = column;
            this.nullSet = new ObjectOpenHashSet<T>();
        }

        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                Set<T> set = getValueSet((K) value);
                if (set == null) {
                    set = addValue((K) value);
                }
                value = ((ValueSet) set).value;

                if (set.add(element)) {
                    elements++;
                }
            }
            return value;
        }

        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                Set<T> set = getValueSet((K) value);
                if (set.remove(element)) {
                    elements--;
                }
                if (set.isEmpty()) {
                    removeValue((K) value);
                }
            }
        }

        public Object replaceValue(T element, K oldValue, K newValue) {
            removeValue(element, oldValue);
            return putValue(element, newValue);
        }

        public int getCount(K value) {
            if (value == null) {
                return nullSet.size();
            }
            Set<T> valueSet = getValueSet(value);
            if (valueSet != null) {
                return valueSet.size();
            } else {
                return 0;
            }
        }

        public Collection values() {
            return new WithNullDecorator();
        }

        public int countValues() {
            return (nullSet.isEmpty() ? 0 : 1) + map.size();
        }

        public Number getMinValue() {
            if (isSortable()) {
                if (map.isEmpty()) {
                    return null;
                } else {
                    return (Number) ((SortedMap) map).firstKey();
                }
            } else {
                throw new UnsupportedOperationException("is not a sortable column.");
            }
        }

        public Number getMaxValue() {
            if (isSortable()) {
                if (map.isEmpty()) {
                    return null;
                } else {
                    return (Number) ((SortedMap) map).lastKey();
                }
            } else {
                throw new UnsupportedOperationException(" is not a sortable column.");
            }
        }

        protected void destroy() {
            map = null;
            nullSet.clear();
            elements = 0;
        }

        protected void clear() {
            map.clear();
            nullSet.clear();
            elements = 0;
        }

        @Override
        public Iterator<Map.Entry<K, Set<T>>> iterator() {
            return new EntryIterator();
        }

        protected Set<T> getValueSet(K value) {
            if (value == null) {
                return nullSet;
            }
            return map.get(value);
        }

        protected void removeValue(K value) {
            map.remove(value);
        }

        protected Set<T> addValue(K value) {
            ValueSet valueSet = new ValueSet(value);
            map.put(value, valueSet);
            return valueSet;
        }

        protected boolean isSortable() {
            return Number.class.isAssignableFrom(column.getTypeClass());
        }

        protected final class WithNullDecorator implements Collection<K> {

            private boolean hasNull() {
                return !nullSet.isEmpty();
            }

            @Override
            public int size() {
                return (hasNull() ? 1 : 0) + map.size();
            }

            @Override
            public boolean isEmpty() {
                return !hasNull() && map.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                if (o == null && hasNull()) {
                    return true;
                } else if (o != null) {
                    return map.containsKey((K) o);
                }
                return false;
            }

            @Override
            public Iterator iterator() {
                return new WithNullIterator();
            }

            @Override
            public Object[] toArray() {
                if (hasNull()) {
                    Object[] res = new Object[map.size() + 1];
                    res[0] = null;
                    System.arraycopy(map.keySet().toArray(), 0, res, 1, map.size());
                    return res;
                } else {
                    return map.keySet().toArray();
                }
            }

            @Override
            public Object[] toArray(Object[] array) {

                if (hasNull()) {
                    if (array.length < size()) {
                        array = (K[]) java.lang.reflect.Array.newInstance(array.getClass().getComponentType(), map.size() + 1);
                    }
                    array[0] = null;
                    System.arraycopy(map.keySet().toArray(), 0, array, 1, map.size());
                    return array;
                } else {
                    return map.keySet().toArray(array);
                }
            }

            @Override
            public boolean add(Object e) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public boolean containsAll(Collection clctn) {
                for (Object o : clctn) {
                    if (o == null && nullSet.isEmpty()) {
                        return false;
                    } else if (o != null && !map.containsKey((K) o)) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean addAll(Collection clctn) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public boolean removeAll(Collection clctn) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public boolean retainAll(Collection clctn) {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Not supported");
            }

            private final class WithNullIterator implements Iterator<K> {

                private final Iterator<K> mapIterator;
                private boolean hasNull;

                public WithNullIterator() {
                    hasNull = hasNull();
                    mapIterator = map.keySet().iterator();
                }

                @Override
                public boolean hasNext() {
                    if (hasNull) {
                        return true;
                    }
                    return mapIterator.hasNext();
                }

                @Override
                public K next() {
                    if (hasNull) {
                        hasNull = false;
                        return null;
                    }
                    return mapIterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Not supported operation.");
                }
            }
        }

        private final class EntryIterator implements Iterator<Map.Entry<K, Set<T>>> {

            private final Iterator<Map.Entry<K, Set<T>>> mapIterator;
            private NullEntry nullEntry;

            public EntryIterator() {
                if (!nullSet.isEmpty()) {
                    nullEntry = new NullEntry();
                }
                mapIterator = map.entrySet().iterator();
            }

            @Override
            public boolean hasNext() {
                if (nullEntry != null) {
                    return true;
                }
                return mapIterator.hasNext();
            }

            @Override
            public Map.Entry<K, Set<T>> next() {
                if (nullEntry != null) {
                    NullEntry ne = nullEntry;
                    nullEntry = null;
                    return ne;
                }
                return mapIterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported operation.");
            }
        }

        private class NullEntry implements Map.Entry<K, Set<T>> {

            @Override
            public K getKey() {
                return null;
            }

            @Override
            public Set<T> getValue() {
                return nullSet;
            }

            @Override
            public Set<T> setValue(Set<T> v) {
                throw new UnsupportedOperationException("Not supported operation.");
            }
        }
    }

    private static final class ValueSet<K, T> implements Set<T> {

        private final K value;
        private final Set<T> set;

        public ValueSet(K value) {
            this.value = value;
            this.set = new ObjectOpenHashSet<T>();
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return set.contains(o);
        }

        @Override
        public Iterator<T> iterator() {
            return set.iterator();
        }

        @Override
        public Object[] toArray() {
            return set.toArray();
        }

        @Override
        public <T> T[] toArray(T[] ts) {
            return set.toArray(ts);
        }

        @Override
        public boolean add(T e) {
            return set.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return set.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> clctn) {
            return set.containsAll(clctn);
        }

        @Override
        public boolean addAll(Collection<? extends T> clctn) {
            throw new UnsupportedOperationException("Not supported operation.");
        }

        @Override
        public boolean retainAll(Collection<?> clctn) {
            throw new UnsupportedOperationException("Not supported operation.");
        }

        @Override
        public boolean removeAll(Collection<?> clctn) {
            throw new UnsupportedOperationException("Not supported operation.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported operation.");
        }

        @Override
        public boolean equals(Object o) {
            return set.equals(o);
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }
    }

    protected class DefaultIndex extends AbstractIndex<Object> {

        public DefaultIndex(ColumnImpl column) {
            super(column);

            map = new Object2ObjectOpenHashMap<Object, Set<T>>();
        }
    }

    protected class BooleanIndex extends AbstractIndex<Boolean> {

        private final Collection values;
        private ValueSet trueSet;
        private ValueSet falseSet;

        public BooleanIndex(ColumnImpl column) {
            super(column);
            trueSet = new ValueSet(Boolean.TRUE);
            falseSet = new ValueSet(Boolean.FALSE);
            values = Arrays.asList(new Object[]{Boolean.TRUE, Boolean.FALSE});
        }

        @Override
        protected Set<T> getValueSet(Boolean value) {
            if (value.equals(Boolean.TRUE)) {
                return trueSet;
            } else {
                return falseSet;
            }
        }

        @Override
        protected Set<T> addValue(Boolean value) {
            throw new RuntimeException("Not supposed to call that");
        }

        @Override
        protected void removeValue(Boolean value) {
            // just ignore, as called via removeValue
        }

        @Override
        public int countValues() {
            return (trueSet.isEmpty() ? 0 : 1) + (falseSet.isEmpty() ? 0 : 1);
        }

        @Override
        public Collection values() {
            return values;
        }

        @Override
        protected void destroy() {
            super.destroy();
            trueSet = new ValueSet(Boolean.TRUE);
            falseSet = new ValueSet(Boolean.FALSE);
        }

        @Override
        protected void clear() {
            trueSet = new ValueSet(Boolean.TRUE);
            falseSet = new ValueSet(Boolean.FALSE);
            elements = 0;
            nullSet.clear();
        }
    }

    protected class DoubleIndex extends AbstractIndex<Double> {

        public DoubleIndex(ColumnImpl column) {
            super(column);

            map = new Double2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class IntegerIndex extends AbstractIndex<Integer> {

        public IntegerIndex(ColumnImpl column) {
            super(column);

            map = new Int2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class FloatIndex extends AbstractIndex<Float> {

        public FloatIndex(ColumnImpl column) {
            super(column);

            map = new Float2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class LongIndex extends AbstractIndex<Long> {

        public LongIndex(ColumnImpl column) {
            super(column);

            map = new Long2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class ShortIndex extends AbstractIndex<Short> {

        public ShortIndex(ColumnImpl column) {
            super(column);

            map = new Short2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class ByteIndex extends AbstractIndex<Byte> {

        public ByteIndex(ColumnImpl column) {
            super(column);

            map = new Byte2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class CharIndex extends AbstractIndex<Character> {

        public CharIndex(ColumnImpl column) {
            super(column);

            map = new Char2ObjectAVLTreeMap<Set<T>>();
        }
    }

    protected class DefaultArrayIndex extends DefaultIndex {

        public DefaultArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (Object s : (Object[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (Object s : (Object[]) value) {
                    super.removeValue(element, s);
                }
            }
        }
    }

    protected class BooleanArrayIndex extends BooleanIndex {

        public BooleanArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (boolean s : (boolean[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (boolean s : (boolean[]) value) {
                    super.removeValue(element, s);
                }
            }
        }
    }

    protected class DoubleArrayIndex extends DoubleIndex {

        public DoubleArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (double s : (double[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (double s : (double[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class IntegerArrayIndex extends IntegerIndex {

        public IntegerArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (int s : (int[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (int s : (int[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class FloatArrayIndex extends FloatIndex {

        public FloatArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (float s : (float[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (float s : (float[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class LongArrayIndex extends LongIndex {

        public LongArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (long s : (long[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (long s : (long[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class ShortArrayIndex extends ShortIndex {

        public ShortArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (short s : (short[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (short s : (short[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class ByteArrayIndex extends ByteIndex {

        public ByteArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (byte s : (byte[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (byte s : (byte[]) value) {
                    super.removeValue(element, s);
                }
            }
        }

        @Override
        protected boolean isSortable() {
            return true;
        }
    }

    protected class CharArrayIndex extends CharIndex {

        public CharArrayIndex(Column column) {
            super((ColumnImpl) column);
        }

        @Override
        public Object putValue(T element, Object value) {
            if (value == null) {
                if (nullSet.add(element)) {
                    elements++;
                }
            } else {
                for (char s : (char[]) value) {
                    super.putValue(element, s);
                }
            }
            return value;
        }

        @Override
        public void removeValue(T element, Object value) {
            if (value == null) {
                if (nullSet.remove(element)) {
                    elements--;
                }
            } else {
                for (char s : (char[]) value) {
                    super.removeValue(element, s);
                }
            }
        }
    }

    private class LockableIterable<T> implements Iterable<T> {

        private final Iterable<T> ite;

        public LockableIterable(Iterable<T> ite) {
            this.ite = ite;
        }

        @Override
        public Iterator<T> iterator() {
            return new LockableIterator<T>(ite.iterator());
        }
    }

    private class LockableIterator<T> implements Iterator<T> {

        private final Iterator<T> itr;

        public LockableIterator(Iterator<T> itr) {
            this.itr = itr;
        }

        @Override
        public boolean hasNext() {
            boolean n = itr.hasNext();
            if (!n) {
                lock.unlock();
            }
            return n;
        }

        @Override
        public T next() {
            return itr.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }
}
