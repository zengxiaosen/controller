/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.util;

import java.net.URI;
import java.util.Date;
import java.util.List;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.model.api.type.IntegerTypeDefinition;
import org.opendaylight.controller.yang.model.api.type.RangeConstraint;

/**
 * Implementation of Yang int32 built-in type. <br>
 * int32 represents integer values between -2147483648 and 2147483647,
 * inclusively. The Java counterpart of Yang int32 built-in type is
 * {@link Integer}.
 *
 * @see AbstractSignedInteger
 *
 */
public class Int32 extends AbstractSignedInteger {

    private static final QName name = BaseTypes.constructQName("int32");
    private Integer defaultValue = null;
    private static final String description =
            "int32  represents integer values between -2147483648 and 2147483647, inclusively.";
    private final IntegerTypeDefinition baseType;

    private Int32() {
        super(name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = this;
    }

    public Int32(final List<String> actualPath, final URI namespace,
            final Date revision) {
        super(actualPath, namespace, revision, name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = new Int32();
    }

    public Int32(final List<String> actualPath, final URI namespace,
            final Date revision, final Integer defaultValue) {
        super(actualPath, namespace, revision, name, description, Integer.MIN_VALUE, Integer.MAX_VALUE, "");
        this.baseType = new Int32();
        this.defaultValue = defaultValue;
    }

    public Int32(final List<String> actualPath, final URI namespace,
            final Date revision, final List<RangeConstraint> rangeStatements,
            final String units, final Integer defaultValue) {
        super(actualPath, namespace, revision, name, description, rangeStatements, units);
        this.baseType = new Int32();
        this.defaultValue = defaultValue;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getBaseType()
     */
    @Override
    public IntegerTypeDefinition getBaseType() {
        return baseType;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.opendaylight.controller.yang.model.api.TypeDefinition#getDefaultValue
     * ()
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Int32 other = (Int32) obj;
        if (defaultValue == null) {
            if (other.defaultValue != null) {
                return false;
            }
        } else if (!defaultValue.equals(other.defaultValue)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Int32 [defaultValue=");
        builder.append(defaultValue);
        builder.append(", AbstractInteger=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }
}
