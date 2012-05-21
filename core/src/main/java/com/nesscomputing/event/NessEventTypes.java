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

public final class NessEventTypes
{
    private NessEventTypes()
    {
    }

    /* Client actions */
    public static final NessEventType WAKE = new NessEventType("WAKE");
    public static final NessEventType SEARCH = new NessEventType("SEARCH");
    public static final NessEventType RATED = new NessEventType("RATED");
    public static final NessEventType COMMENTED = new NessEventType("COMMENTED");

    /* User lifecycle */
    public static final NessEventType USER_REGISTERED = new NessEventType("USER_REGISTERED");
    public static final NessEventType USER_USING_UDID = new NessEventType("USER_USING_UDID");

    /* Changes to user data */
    public static final NessEventType FRIENDSHIP_CHANGED = new NessEventType("FRIENDSHIP_CHANGED");
}
