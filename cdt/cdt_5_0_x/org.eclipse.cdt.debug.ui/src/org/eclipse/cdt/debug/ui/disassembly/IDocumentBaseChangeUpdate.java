/*******************************************************************************
 * Copyright (c) 2008 ARM Limited and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * ARM Limited - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.debug.ui.disassembly;

/**
 * Request to provide a base element for the given element and presentation context.
 * <p>
 * Clients are not intended to implement this interface.
 * </p>
 * 
 * This interface is experimental
 */
public interface IDocumentBaseChangeUpdate extends IDocumentUpdate {

    /**
     * Returns the offset of the old base element.
     * 
     * @return the offset of the old base element
     */
    public int getOriginalOffset();

    /**
     * Sets the base element to use with the given presentation context.
     * 
     * @param base the base element to use with the given presentation context
     */
    public void setBaseElement( Object base );

    /**
     * Sets the offset of the new base element.
     * 
     * @param offset the offset of the new base element
     */
    public void setOffset( int offset );
}
