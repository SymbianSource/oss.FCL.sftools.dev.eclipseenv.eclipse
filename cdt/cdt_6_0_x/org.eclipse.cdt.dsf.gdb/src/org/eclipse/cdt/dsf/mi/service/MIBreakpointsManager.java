/*******************************************************************************
 * Copyright (c) 2007, 2008 Wind River and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River - Initial API and implementation
 *     Ericsson   - High-level breakpoints integration
 *     Ericsson   - Added breakpoint filter support
 *     Ericsson   - Re-factored the service and put a few comments
 *     Ericsson   - Added Action support
 *******************************************************************************/

package org.eclipse.cdt.dsf.mi.service;

import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator;
import org.eclipse.cdt.dsf.service.DsfSession;

/**
 * Breakpoint service interface.  The breakpoint service tracks CDT breakpoint
 * objects, and based on those, it manages breakpoints in the debugger back end.
 * 
 * It relies on MIBreakpoints for the actual back-end interface.
 */
public class MIBreakpointsManager extends BreakpointsMediator
{
    /**
     * The service constructor.
     * Performs basic instantiation (method initialize() performs the real
     * service initialization asynchronously).
     * 
     * @param session       the debugging session
     * @param debugModelId  the debugging model
     */
    public MIBreakpointsManager(DsfSession session, String debugModelId) {
        super(session, new MIBreakpointAttributeTranslator(session, debugModelId));
    }
}
