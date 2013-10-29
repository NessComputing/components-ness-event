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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * Describes an Event in the Ness platform.
 */
@Immutable
public class NessEvent
{
    public static final int EVENT_VERSION = 2;

    private final Map<String, ? extends Object> payload;
    private final NessEventType type;
    private final UUID user;
    private final UUID id;

    /** the time when this event entered the system */
    private final DateTime timestamp;

    /**
     * Create a new event from over-the-wire json.
     *
     * @param user      User that the event happened for. Can be null for a system level event.
     * @param timestamp The time when this event entered the system
     * @param type      The Event type.
     * @param payload   Arbitrary data describing the event.
     * @param id        UUID as event id.
     */
    @JsonCreator
    static NessEvent createEvent(@Nullable @JsonProperty("user") final UUID user,
                                        @Nullable @JsonProperty("timestamp") final DateTime timestamp,
                                        @Nonnull @JsonProperty("id") final UUID id,
                                        @Nonnull @JsonProperty("type") final NessEventType type,
                                        @Nullable @JsonProperty("payload") final Map<String, ? extends Object> payload)
    {
        return new NessEvent(user, timestamp, type, payload, id);
    }

    /**
     * Create a new event.
     *
     * @param user      User that the event happened for. Can be null for a system level event.
     * @param timestamp The time when this event entered the system
     * @param type      The Event type.
     * @param payload   Arbitrary data describing the event.
     */
    public static NessEvent createEvent(@Nullable final UUID user,
                                        @Nullable final DateTime timestamp,
                                        @Nonnull final NessEventType type,
                                        @Nullable final Map<String, ? extends Object> payload)
    {
        return new NessEvent(user, timestamp, type, payload, UUID.randomUUID());
    }


    /**
     * Convenience constructor that assigns the current time in UTC and a random UUID.
     */
    public static NessEvent createEvent(@Nullable final UUID user,
                                        @Nonnull final NessEventType type,
                                        @Nullable final Map<String, ? extends Object> payload)
    {
        return new NessEvent(user, new DateTime(DateTimeZone.UTC), type, payload, UUID.randomUUID());
    }

    /**
     * Convenience constructor that assumes no payload.
     */
    public static NessEvent createEvent(@Nullable final UUID user,
                                        @Nonnull final NessEventType type)
    {
        return new NessEvent(user, new DateTime(DateTimeZone.UTC), type, Collections.<String, Object>emptyMap(), UUID.randomUUID());
    }

    /**
     * Create a new event.
     *
     * @param user		 user that the event happend for
     * @param timestamp the time when this event entered the system
     * @param type	  event type
     * @param payload		arbitrary data
     * @param id		   system-assigned uuid
     */
    NessEvent(@Nullable final UUID user,
              @Nullable final DateTime timestamp,
              @Nonnull final NessEventType type,
              @Nullable final Map<String, ? extends Object> payload,
              @Nonnull final UUID id)
    {
        Preconditions.checkArgument(id != null, "id must not be null!");
        Preconditions.checkArgument(type != null, "type must not be null!");

        this.user = user;
        this.timestamp = (timestamp == null) ? new DateTime(DateTimeZone.UTC) : timestamp;
        this.type = type;
        this.id = id;
        this.payload = (payload == null) ? Collections.<String, Object>emptyMap() : new HashMap<String, Object>(payload);
    }

    @Nonnull
    public NessEventType getType()
    {
        return type;
    }

    @CheckForNull
    public UUID getUser()
    {
        return user;
    }

    @Nonnull
    public DateTime getTimestamp()
    {
        return timestamp;
    }

    @Nonnull
    public Map<String, ? extends Object> getPayload()
    {
        return payload;
    }

    @Nonnull
    public UUID getId()
    {
        return id;
    }

    @JsonProperty("v")
    public int getVersion()
    {
        return EVENT_VERSION;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof NessEvent)) {
            return false;
        }
        NessEvent castOther = (NessEvent) other;
        return new EqualsBuilder().append(payload, castOther.payload).append(type, castOther.type).append(user, castOther.user).append(id, castOther.id).append(timestamp, castOther.timestamp).isEquals();
    }

    private transient int hashCode;

    @Override
    public int hashCode()
    {
        if (hashCode == 0) {
            hashCode = new HashCodeBuilder().append(payload).append(type).append(user).append(id).append(timestamp).toHashCode();
        }
        return hashCode;
    }

    private transient String toString;

    @Override
    public String toString()
    {
        if (toString == null) {
            toString = new ToStringBuilder(this).append("payload", payload).append("type", type).append("user", user).append("id", id).append("timestamp", timestamp).toString();
        }
        return toString;
    }


}
