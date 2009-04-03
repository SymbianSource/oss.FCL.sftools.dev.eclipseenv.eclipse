/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.cdt.core.parser.tests;

import junit.framework.TestCase;

import org.eclipse.cdt.core.parser.util.CharArrayObjectMap;

/**
 * @author Doug Schaefer
 */
public class CharArrayUtilsTest extends TestCase {

	public void testMapAdd() {
		CharArrayObjectMap map = new CharArrayObjectMap(4);
		char[] key1 = "key1".toCharArray();
		Object value1 = new Integer(43);
		map.put(key1, value1);
		
		char[] key2 = "key1".toCharArray();
		Object value2 = map.get(key2);
		assertEquals(value1, value2);
		
		for (int i = 0; i < 5; ++i) {
			map.put(("ikey" + i).toCharArray(), new Integer(i));
		}
		
		Object ivalue1 = map.get("ikey1".toCharArray());
		assertEquals(ivalue1, new Integer(1));
		
		Object ivalue4 = map.get("ikey4".toCharArray());
		assertEquals(ivalue4, new Integer(4));
	}
}
