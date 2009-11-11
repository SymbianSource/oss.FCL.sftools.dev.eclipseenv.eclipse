/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.debug.service;

import java.util.List;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.ThreadSafeAndProhibitedFromDsfExecutor;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.BreakpointEventType;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.ITargetBreakpointInfo;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Breakpoint attribute translator interface
 * 
 * @since 2.1
 */

@ThreadSafeAndProhibitedFromDsfExecutor("")
public interface IBreakpointAttributeTranslatorExtension extends IBreakpointAttributeTranslator {
	/**
	 * Convert all attributes of the given platform breakpoint (BP) to
	 * attributes of potential target breakpoints. Two tasks are involved:<br>
	 * 1. Convert the attributes to debugger specific ones, if needed. For
	 * instance, GDB implementation has its own breakpoint attribute keys. <br>
	 * 2. Resolve the breakpoint. A platform BP may be mapped to two or more
	 * target BPs, e.g. a breakpoint in an in-line function may be mapped to
	 * several target BPs, or a thread-specific BP may be mapped to several
	 * target BPs each of which is for one thread. This method will return an
	 * attribute map for each of the target BP.<br>
	 * <br>
	 * This method must be called in DSF execution thread.
	 * 
	 * @param context
	 *            - a IBreakpointsTargetDMContext object (which could be a
	 *            process or a loaded module) in which we locate target BPs for
	 *            the platform BP.
	 * @param breakpoint
	 *            - platform breakpoint.
	 * @param bpManagerEnabled
	 *            - flag from platform breakpoint manager indicating that all
	 *            breakpoints are enabled.
	 * @param drm
	 *            - on completion of the request, the DataRequestMonitor
	 *            contains one or more attribute maps each of which
	 *            corresponding to one target breakpoint.
	 * @throws CoreException
	 */
    public void getTargetBreakpointAttributes(IBreakpointsTargetDMContext context, IBreakpoint breakpoint, 
    		boolean bpManagerEnabled, DataRequestMonitor<List<Map<String, Object>>> drm);

    /**
     * Convert platform breakpoint attributes to target-recognizable attributes. 
     * This method does not perform task #2 done by {@link this#getTargetBreakpointAttributes(List, IBreakpoint, boolean)}.
     * 
     * @param platformBPAttrDelta
     * @return
     */
    public Map<String, Object> convertAttributeDelta(Map<String, Object> platformBPAttrDelta);
        
    /**
     * Update platform about breakpoint status change, e.g. breakpoint installed on target successfully or breakpoint
     * removed from target successfully.
     *  
     * @param bpsInfo
     * @param eventType
     */
    public void updateBreakpointsStatus(Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> bpsInfo, BreakpointEventType eventType);
}