/*
 * This file is part of FastClasspathScanner.
 *
 * Author: Luke Hutchison
 *
 * Hosted at: https://github.com/lukehutch/fast-classpath-scanner
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Luke Hutchison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.lukehutch.fastclasspathscanner.scanner;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult.InfoObject;
import io.github.lukehutch.fastclasspathscanner.typesignature.TypeSignature;
import io.github.lukehutch.fastclasspathscanner.utils.Parser.ParseException;

/** Holds metadata about annotations. */
public class AnnotationInfo extends InfoObject implements Comparable<AnnotationInfo> {
    String annotationName;
    List<AnnotationParamValue> annotationParamValues;
    transient ScanResult scanResult;

    AnnotationInfo() {
    }

    @Override
    void setScanResult(final ScanResult scanResult) {
        this.scanResult = scanResult;
        if (annotationParamValues != null) {
            for (final AnnotationParamValue a : annotationParamValues) {
                if (a != null) {
                    a.setScanResult(scanResult);
                }
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A union type, used for recovering object type during deserialization. Only one field is ever set. */
    private static class AnnotationParamValueWrapper extends InfoObject {
        // Parameter value is split into different fields by type, so that serialization and deserialization
        // works properly (can't properly serialize a field of Object type, since the concrete type is not
        // stored in JSON).
        AnnotationEnumValue annotationEnumValue;
        AnnotationClassRef annotationClassRef;
        AnnotationInfo annotationInfo;
        AnnotationParamValueWrapper[] annotationValueArray;
        String annotationConstantString;
        Integer annotationConstantInteger;
        Long annotationConstantLong;
        Short annotationConstantShort;
        Boolean annotationConstantBoolean;
        Character annotationConstantCharacter;
        Float annotationConstantFloat;
        Double annotationConstantDouble;
        Byte annotationConstantByte;

        @SuppressWarnings("unused")
        public AnnotationParamValueWrapper() {
        }

        public AnnotationParamValueWrapper(final Object annotationParamValue) {
            if (annotationParamValue != null) {
                if (annotationParamValue.getClass().isArray()) {
                    final int n = Array.getLength(annotationParamValue);
                    annotationValueArray = new AnnotationParamValueWrapper[n];
                    for (int i = 0; i < n; i++) {
                        annotationValueArray[i] = new AnnotationParamValueWrapper(
                                Array.get(annotationParamValue, i));
                    }
                } else if (annotationParamValue instanceof AnnotationEnumValue) {
                    annotationEnumValue = (AnnotationEnumValue) annotationParamValue;
                } else if (annotationParamValue instanceof AnnotationClassRef) {
                    annotationClassRef = (AnnotationClassRef) annotationParamValue;
                } else if (annotationParamValue instanceof AnnotationInfo) {
                    annotationInfo = (AnnotationInfo) annotationParamValue;
                } else if (annotationParamValue instanceof String) {
                    annotationConstantString = (String) annotationParamValue;
                } else if (annotationParamValue instanceof Integer) {
                    annotationConstantInteger = (Integer) annotationParamValue;
                } else if (annotationParamValue instanceof Long) {
                    annotationConstantLong = (Long) annotationParamValue;
                } else if (annotationParamValue instanceof Short) {
                    annotationConstantShort = (Short) annotationParamValue;
                } else if (annotationParamValue instanceof Boolean) {
                    annotationConstantBoolean = (Boolean) annotationParamValue;
                } else if (annotationParamValue instanceof Character) {
                    annotationConstantCharacter = (Character) annotationParamValue;
                } else if (annotationParamValue instanceof Float) {
                    annotationConstantFloat = (Float) annotationParamValue;
                } else if (annotationParamValue instanceof Double) {
                    annotationConstantDouble = (Double) annotationParamValue;
                } else if (annotationParamValue instanceof Byte) {
                    annotationConstantByte = (Byte) annotationParamValue;
                } else {
                    throw new IllegalArgumentException("Unsupported annotation parameter value type: "
                            + annotationParamValue.getClass().getName());
                }
            }
        }

        /** Unwrap the wrapped value. */
        public Object get() {
            if (annotationValueArray != null) {
                final Object[] annotationValueObjects = new Object[annotationValueArray.length];
                for (int i = 0; i < annotationValueArray.length; i++) {
                    if (annotationValueArray[i] != null) {
                        annotationValueObjects[i] = annotationValueArray[i].get();
                    }
                }
                return annotationValueObjects;
            } else if (annotationEnumValue != null) {
                return annotationEnumValue;
            } else if (annotationClassRef != null) {
                return annotationClassRef;
            } else if (annotationInfo != null) {
                return annotationInfo;
            } else if (annotationConstantString != null) {
                return annotationConstantString;
            } else if (annotationConstantInteger != null) {
                return annotationConstantInteger;
            } else if (annotationConstantLong != null) {
                return annotationConstantLong;
            } else if (annotationConstantShort != null) {
                return annotationConstantShort;
            } else if (annotationConstantBoolean != null) {
                return annotationConstantBoolean;
            } else if (annotationConstantCharacter != null) {
                return annotationConstantCharacter;
            } else if (annotationConstantFloat != null) {
                return annotationConstantFloat;
            } else if (annotationConstantDouble != null) {
                return annotationConstantDouble;
            } else if (annotationConstantByte != null) {
                return annotationConstantByte;
            } else {
                return null;
            }
        }

        @Override
        void setScanResult(final ScanResult scanResult) {
            if (annotationValueArray != null) {
                for (int i = 0; i < annotationValueArray.length; i++) {
                    if (annotationValueArray[i] != null) {
                        annotationValueArray[i].setScanResult(scanResult);
                    }
                }
            } else if (annotationEnumValue != null) {
                annotationEnumValue.setScanResult(scanResult);
            } else if (annotationClassRef != null) {
                annotationClassRef.setScanResult(scanResult);
            } else if (annotationInfo != null) {
                annotationInfo.setScanResult(scanResult);
            }
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /** A wrapper used to pair annotation parameter names with annotation parameter values. */
    public static class AnnotationParamValue extends InfoObject implements Comparable<AnnotationParamValue> {
        String paramName;
        AnnotationParamValueWrapper paramValue;

        AnnotationParamValue() {
        }

        /**
         * @param paramName
         *            The annotation paramater name.
         * @param paramValue
         *            The annotation parameter value.
         */
        public AnnotationParamValue(final String paramName, final Object paramValue) {
            this.paramName = paramName;
            this.paramValue = new AnnotationParamValueWrapper(paramValue);
        }

        @Override
        void setScanResult(final ScanResult scanResult) {
            if (paramValue != null) {
                paramValue.setScanResult(scanResult);
            }
        }

        /**
         * Get the annotation parameter name.
         * 
         * @return The annotation parameter name.
         */
        public String getParamName() {
            return paramName;
        }

        /**
         * Get the annotation parameter value.
         * 
         * @return The annotation parameter value. May be one of the following types:
         *         <ul>
         *         <li>String for string constants
         *         <li>A wrapper type, e.g. Integer or Character, for primitive-typed constants
         *         <li>{@link Object}[] for array types (and then the array element type may be one of the types in
         *         this list)
         *         <li>{@link AnnotationEnumValue}, for enum constants (this wraps the enum class and the string
         *         name of the constant)
         *         <li>{@link AnnotationClassRef}, for Class references within annotations (this wraps the name of
         *         the referenced class)
         *         <li>{@link AnnotationInfo}, for nested annotations
         *         </ul>
         */
        public Object getParamValue() {
            return paramValue == null ? null : paramValue.get();
        }

        @Override
        public String toString() {
            final StringBuilder buf = new StringBuilder();
            toString(buf);
            return buf.toString();
        }

        void toString(final StringBuilder buf) {
            buf.append(paramName);
            buf.append(" = ");
            toStringParamValueOnly(buf);
        }

        void toStringParamValueOnly(final StringBuilder buf) {
            if (paramValue == null) {
                buf.append("null");
            } else {
                final Object paramVal = paramValue.get();
                final Class<? extends Object> valClass = paramVal.getClass();
                if (valClass.isArray()) {
                    buf.append('{');
                    for (int j = 0, n = Array.getLength(paramVal); j < n; j++) {
                        if (j > 0) {
                            buf.append(", ");
                        }
                        final Object elt = Array.get(paramVal, j);
                        buf.append(elt == null ? "null" : elt.toString());
                    }
                    buf.append('}');
                } else if (paramVal instanceof String) {
                    buf.append('"');
                    buf.append(paramVal.toString().replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"));
                    buf.append('"');
                } else if (paramVal instanceof Character) {
                    buf.append('\'');
                    buf.append(paramVal.toString().replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r"));
                    buf.append('\'');
                } else {
                    buf.append(paramVal.toString());
                }
            }
        }

        @Override
        public int compareTo(final AnnotationParamValue o) {
            final int diff = paramName.compareTo(o.getParamName());
            if (diff != 0) {
                return diff;
            }
            // Use toString() order and get() (which can be slow) as a last-ditch effort -- only happens
            // if the annotation has multiple parameters of the same name but different value. 
            final Object p0 = getParamValue();
            final Object p1 = o.getParamValue();
            if (p0 == null && p1 == null) {
                return 0;
            } else if (p0 == null && p1 != null) {
                return -1;
            } else if (p0 != null && p1 == null) {
                return 1;
            }
            return p0.toString().compareTo(p1.toString());
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof AnnotationParamValue)) {
                return false;
            }
            final AnnotationParamValue o = (AnnotationParamValue) obj;
            final int diff = this.compareTo(o);
            return (diff != 0 ? false
                    : paramValue == null && o.paramValue == null ? true
                            : paramValue == null || o.paramValue == null ? false : true);
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /**
     * Class for wrapping an enum constant value (split into class name and constant name) referenced inside an
     * annotation.
     */
    public static class AnnotationEnumValue extends InfoObject implements Comparable<AnnotationEnumValue> {
        String className;
        String constName;
        transient ScanResult scanResult;

        AnnotationEnumValue() {
        }

        /**
         * @param className
         *            The enum class name.
         * @param constName
         *            The enum const name.
         */
        public AnnotationEnumValue(final String className, final String constName) {
            this.className = className;
            this.constName = constName;
        }

        @Override
        void setScanResult(final ScanResult scanResult) {
            this.scanResult = scanResult;
        }

        /**
         * Get the class name of the enum.
         * 
         * @return The name of the enum class.
         */
        public String getClassName() {
            return className;
        }

        /**
         * Get the name of the enum constant.
         * 
         * @return The name of the enum constant.
         */
        public String getConstName() {
            return constName;
        }

        /**
         * Get the enum constant. Causes the ClassLoader to load the enum class.
         * 
         * @return A ref to the enum constant value.
         * @throws IllegalArgumentException
         *             if the class could not be loaded, or the enum constant is invalid.
         */
        public Object getEnumValueRef() throws IllegalArgumentException {
            final Class<?> classRef = scanResult.classNameToClassRef(className);
            if (!classRef.isEnum()) {
                throw new IllegalArgumentException("Class " + className + " is not an enum");
            }
            Field field;
            try {
                field = classRef.getDeclaredField(constName);
            } catch (NoSuchFieldException | SecurityException e) {
                throw new IllegalArgumentException("Could not find enum constant " + toString(), e);
            }
            if (!field.isEnumConstant()) {
                throw new IllegalArgumentException("Field " + toString() + " is not an enum constant");
            }
            try {
                return field.get(null);
            } catch (final IllegalAccessException e) {
                throw new IllegalArgumentException("Field " + toString() + " is not accessible", e);
            }
        }

        @Override
        public int compareTo(final AnnotationEnumValue o) {
            final int diff = className.compareTo(o.className);
            return diff == 0 ? constName.compareTo(o.constName) : diff;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof AnnotationEnumValue)) {
                return false;
            }
            return compareTo((AnnotationEnumValue) o) == 0;
        }

        @Override
        public int hashCode() {
            return className.hashCode() * 11 + constName.hashCode();
        }

        @Override
        public String toString() {
            return className + "." + constName;
        }
    }

    /**
     * Stores a class descriptor in an annotation as a class type string, e.g. "[[[java/lang/String;" is stored as
     * "String[][][]".
     *
     * <p>
     * Use ReflectionUtils.typeStrToClass() to get a {@code Class<?>} reference from this class type string.
     */
    public static class AnnotationClassRef extends InfoObject {
        String typeDescriptor;
        transient TypeSignature typeSignature;
        transient ScanResult scanResult;

        AnnotationClassRef() {
        }

        @Override
        void setScanResult(final ScanResult scanResult) {
            this.scanResult = scanResult;
        }

        AnnotationClassRef(final String classRefTypeDescriptor) {
            this.typeDescriptor = classRefTypeDescriptor;
        }

        /**
         * Get the type signature for a type reference used in an annotation parameter.
         *
         * <p>
         * Call getType() to get a {@code Class<?>} reference for this class.
         * 
         * @return The type signature of the annotation class ref.
         */
        public TypeSignature getTypeSignature() {
            if (typeSignature == null) {
                try {
                    typeSignature = TypeSignature.parse(typeDescriptor);
                } catch (final ParseException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return typeSignature;
        }

        /**
         * Get a class reference for a class-reference-typed value used in an annotation parameter. Causes the
         * ClassLoader to load the class.
         * 
         * @deprecated in favor of getClassRef(), for consistency with {@code ClassInfo#getClassRef()}.
         * 
         * @return The type signature of the annotation class ref, as a {@code Class<?>} reference.
         */
        @Deprecated
        public Class<?> getType() {
            return getClassRef();
        }

        /**
         * Get a class reference for a class-reference-typed value used in an annotation parameter. Causes the
         * ClassLoader to load the class.
         * 
         * @return The type signature of the annotation class ref, as a {@code Class<?>} reference.
         */
        public Class<?> getClassRef() {
            return getTypeSignature().instantiate(scanResult);
        }

        @Override
        public int hashCode() {
            return getTypeSignature().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof AnnotationClassRef)) {
                return false;
            }
            return getTypeSignature().equals(((AnnotationClassRef) obj).getTypeSignature());
        }

        @Override
        public String toString() {
            return getTypeSignature().toString();
        }
    }

    // -------------------------------------------------------------------------------------------------------------

    /**
     * @param annotationName
     *            The name of the annotation.
     * @param annotationParamValues
     *            The annotation parameter values, or null if none.
     */
    public AnnotationInfo(final String annotationName, final List<AnnotationParamValue> annotationParamValues) {
        this.annotationName = annotationName;
        // Sort the annotation parameter values into order for consistency
        if (annotationParamValues != null) {
            Collections.sort(annotationParamValues);
        }
        this.annotationParamValues = annotationParamValues;
    }

    /**
     * Add a set of default values, stored in an annotation class' classfile, to a concrete instance of that
     * annotation. The defaults are overwritten by any annotation parameter values in the concrete annotation.
     * 
     * @param defaultAnnotationParamValues
     *            the default parameter values for the annotation.
     */
    void addDefaultValues(final List<AnnotationParamValue> defaultAnnotationParamValues) {
        if (defaultAnnotationParamValues != null && !defaultAnnotationParamValues.isEmpty()) {
            if (this.annotationParamValues == null || this.annotationParamValues.isEmpty()) {
                this.annotationParamValues = new ArrayList<>(defaultAnnotationParamValues);
            } else {
                // Overwrite defaults with non-defaults
                final Map<String, Object> allParamValues = new HashMap<>();
                for (final AnnotationParamValue annotationParamValue : defaultAnnotationParamValues) {
                    allParamValues.put(annotationParamValue.paramName, annotationParamValue.paramValue.get());
                }
                for (final AnnotationParamValue annotationParamValue : this.annotationParamValues) {
                    allParamValues.put(annotationParamValue.paramName, annotationParamValue.paramValue.get());
                }
                this.annotationParamValues.clear();
                for (final Entry<String, Object> ent : allParamValues.entrySet()) {
                    this.annotationParamValues.add(new AnnotationParamValue(ent.getKey(), ent.getValue()));
                }
            }
        }
        if (this.annotationParamValues != null) {
            Collections.sort(this.annotationParamValues);
        }
    }

    /**
     * Get the name of the annotation.
     * 
     * @return The annotation name.
     */
    public String getAnnotationName() {
        return annotationName;
    }

    /**
     * Get a class reference for the annotation.
     * 
     * @return The annotation type, as a {@code Class<?>} reference.
     */
    public Class<?> getAnnotationType() {
        return scanResult.classNameToClassRef(annotationName);
    }

    /**
     * Get the parameter value of the annotation.
     * 
     * @return The annotation parameter values.
     */
    public List<AnnotationParamValue> getAnnotationParamValues() {
        return annotationParamValues;
    }

    @Override
    public int compareTo(final AnnotationInfo o) {
        final int diff = annotationName.compareTo(o.annotationName);
        if (diff != 0) {
            return diff;
        }
        if (annotationParamValues == null && o.annotationParamValues == null) {
            return 0;
        } else if (annotationParamValues == null) {
            return -1;
        } else if (o.annotationParamValues == null) {
            return 1;
        } else {
            for (int i = 0, max = Math.max(annotationParamValues.size(),
                    o.annotationParamValues.size()); i < max; i++) {
                if (i >= annotationParamValues.size()) {
                    return -1;
                } else if (i >= o.annotationParamValues.size()) {
                    return 1;
                } else {
                    final int diff2 = annotationParamValues.get(i).compareTo(o.annotationParamValues.get(i));
                    if (diff2 != 0) {
                        return diff2;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AnnotationInfo)) {
            return false;
        }
        final AnnotationInfo o = (AnnotationInfo) obj;
        return this.compareTo(o) == 0;
    }

    @Override
    public int hashCode() {
        int h = annotationName.hashCode();
        if (annotationParamValues != null) {
            for (int i = 0; i < annotationParamValues.size(); i++) {
                final AnnotationParamValue e = annotationParamValues.get(i);
                h = h * 7 + e.getParamName().hashCode() * 3 + e.getParamValue().hashCode();
            }
        }
        return h;
    }

    /**
     * Render as a string, into a StringBuilder buffer.
     * 
     * @param buf
     *            The buffer.
     */
    public void toString(final StringBuilder buf) {
        buf.append("@" + annotationName);
        if (annotationParamValues != null) {
            buf.append('(');
            for (int i = 0; i < annotationParamValues.size(); i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                final AnnotationParamValue annotationParamValue = annotationParamValues.get(i);
                if (annotationParamValues.size() > 1 || !"value".equals(annotationParamValue.paramName)) {
                    annotationParamValue.toString(buf);
                } else {
                    annotationParamValue.toStringParamValueOnly(buf);
                }
            }
            buf.append(')');
        }
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        toString(buf);
        return buf.toString();
    }

    // -------------------------------------------------------------------------------------------------------------

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * From a collection of AnnotationInfo objects, extract the annotation names, uniquify them, and sort them.
     * 
     * @param annotationInfo
     *            The annotation info.
     * @return The sorted, uniquified annotation names.
     */
    public static String[] getUniqueAnnotationNamesSorted(final Collection<AnnotationInfo> annotationInfo) {
        if (annotationInfo == null || annotationInfo.isEmpty()) {
            return EMPTY_STRING_ARRAY;
        }
        final Set<String> annotationNamesSet = new HashSet<>();
        for (final AnnotationInfo annotation : annotationInfo) {
            annotationNamesSet.add(annotation.annotationName);
        }
        final String[] annotationNamesSorted = new String[annotationNamesSet.size()];
        int i = 0;
        for (final String annotationName : annotationNamesSet) {
            annotationNamesSorted[i++] = annotationName;
        }
        Arrays.sort(annotationNamesSorted);
        return annotationNamesSorted;
    }

    /**
     * From an array of AnnotationInfo objects, extract the annotation names, uniquify them, and sort them.
     * 
     * @param annotationInfo
     *            The annotation info.
     * @return The sorted, uniquified annotation names.
     */
    public static String[] getUniqueAnnotationNamesSorted(final AnnotationInfo[] annotationInfo) {
        if (annotationInfo == null || annotationInfo.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        final Set<String> annotationNamesSet = new HashSet<>();
        for (final AnnotationInfo annotation : annotationInfo) {
            annotationNamesSet.add(annotation.annotationName);
        }
        final String[] annotationNamesSorted = new String[annotationNamesSet.size()];
        int i = 0;
        for (final String annotationName : annotationNamesSet) {
            annotationNamesSorted[i++] = annotationName;
        }
        Arrays.sort(annotationNamesSorted);
        return annotationNamesSorted;
    }
}