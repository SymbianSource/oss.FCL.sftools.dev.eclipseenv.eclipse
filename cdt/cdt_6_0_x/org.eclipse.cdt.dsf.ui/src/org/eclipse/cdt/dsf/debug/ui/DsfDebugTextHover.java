/*******************************************************************************
 * Copyright (c) 2009 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ericsson - initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.debug.ui;


/**
 * An implementation of AbstractDebugTextHover using DSF services.
 * 
 * @since 2.1
 */
public class DsfDebugTextHover extends AbstractDsfDebugTextHover {

    /*
     * Not needed for the default DSF hover
     */
	@Override
    protected String getModelId() { return null; }
    
	@Override
	protected boolean canEvaluate() {
	    if (getFrame() != null) {
	        return true;
	    }
	    return false;
	}
}
