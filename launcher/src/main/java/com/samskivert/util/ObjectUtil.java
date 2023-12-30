//
// $Id$
//
// samskivert library - useful routines for java programs
// Copyright (C) 2001-2011 Michael Bayne, et al.
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.samskivert.util;

import java.lang.reflect.Field;
import java.io.PrintStream;

import java.util.Arrays;

import com.samskivert.annotation.*;

/**
 * Utility methods that don't fit anywhere else.
 */
public class ObjectUtil
{
    /**
     * Cast the specified Object, or return null if it is not an instance.
     */
    public static <T> T as (Object obj, Class<T> clazz)
    {
        return clazz.isInstance(obj) ? clazz.cast(obj) : null;
    }

    /**
     * Test two objects for equality safely.
     */
    @ReplacedBy("com.google.common.base.Objects#equal(Object,Object)")
    public static boolean equals (Object o1, Object o2)
    {
        return (o1 == o2) || ((o1 != null) && o1.equals(o2));
    }
}
