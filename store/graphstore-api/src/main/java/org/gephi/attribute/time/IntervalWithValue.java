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
package org.gephi.attribute.time;

/**
 * Represents an Interval with an associated value for it
 * @author Eduardo Ramos<eduramiba@gmail.com>
 * @param <T>
 */
public final class IntervalWithValue<T> extends Interval {
    
    private final T value;

    public IntervalWithValue(double low, double high, boolean lopen, boolean ropen, T value) {
        super(low, high, lopen, ropen);
        this.value = value;
    }

    public T getValue(){
        return value;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + (this.value != null ? this.value.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IntervalWithValue<?> other = (IntervalWithValue<?>) obj;
        if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
            return false;
        }
        
        return super.equals(obj);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lopen ? '(' : '[');
        sb.append(low);
        sb.append(", ");
        sb.append(high);
        
        if (value != null) {
            sb.append(", ");
            String stringValue = value.toString();
            if (containsSpecialCharacters(stringValue) || stringValue.trim().isEmpty()) {
                sb.append('"');
                sb.append(stringValue.replace("\\", "\\\\").replace("\"", "\\\""));
                sb.append('"');
            } else {
                sb.append(stringValue);
            }
        }

        sb.append(ropen ? ')' : ']');

        return sb.toString();
    }
    
    /**
     * @param value String value
     * @return True if the string contains special characters for dynamic intervals syntax
     */
    public static boolean containsSpecialCharacters(String value) {
        for (Character c : ";,()[]\"'".toCharArray()) {
            if (value.indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }
}
