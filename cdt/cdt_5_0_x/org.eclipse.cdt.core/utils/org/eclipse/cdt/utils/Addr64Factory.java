/*******************************************************************************
 * Copyright (c) 2004, 2005 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Intel Corporation - Initial API and implementation
 ******************************************************************************/
package org.eclipse.cdt.utils;

import java.math.BigInteger;

import org.eclipse.cdt.core.IAddress;
import org.eclipse.cdt.core.IAddressFactory2;

public class Addr64Factory implements IAddressFactory2 {

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory#getZero()
	 */
	public IAddress getZero() {
		return Addr64.ZERO;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory#getMax()
	 */
	public IAddress getMax() {
		return Addr64.MAX;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory#createAddress(java.lang.String)
	 */
	public IAddress createAddress(String addr) {
		return createAddress(addr, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory2#createAddress(java.lang.String, boolean)
	 */
	public IAddress createAddress(String addr, boolean truncate) {
		return new Addr64(addr, truncate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory#createAddress(java.lang.String, int)
	 */
	public IAddress createAddress(String addr, int radix) {
		return createAddress(addr, radix, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory2#createAddress(java.lang.String, int, boolean)
	 */
	public IAddress createAddress(String addr, int radix, boolean truncate) {
		return new Addr64(addr, radix, truncate);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory#createAddress(java.math.BigInteger)
	 */
	public IAddress createAddress(BigInteger addr) {
		return createAddress(addr, true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IAddressFactory2#createAddress(java.math.BigInteger, boolean)
	 */
	public IAddress createAddress(BigInteger addr, boolean truncate) {
		return new Addr64(addr, truncate);
	}
}