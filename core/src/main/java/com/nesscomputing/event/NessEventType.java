/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nesscomputing.event;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


@Immutable
public final class NessEventType
{
    private static final NessEventType UNKNOWN = new NessEventType("");

    private final String name;

    NessEventType(@Nonnull final String name)
    {
        Preconditions.checkArgument(name != null, "event name can not be null!");
        this.name = name.toUpperCase(Locale.ENGLISH);
    }

    @JsonCreator
    public static NessEventType getForName(@Nullable final String name)
    {
        return (name == null) ? UNKNOWN : new NessEventType(name);
    }

    @JsonValue
    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof NessEventType)) {
            return false;
        }
        NessEventType castOther = (NessEventType) other;
        return new EqualsBuilder().append(name, castOther.name).isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(name).toHashCode();
        }
        return hashCode;
    }

    private transient String toString;

    @Override
    public String toString()
    {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("name", name).toString();
        }
        return toString;
    }


}
