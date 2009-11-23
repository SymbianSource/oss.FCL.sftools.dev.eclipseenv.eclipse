/*******************************************************************************
 * Copyright (c) 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Nokia - enhanced to work for both GDB and EDC.  Nov. 2009.
 *******************************************************************************/
package org.eclipse.cdt.dsf.debug.service;

import java.util.List;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.BreakpointEventType;
import org.eclipse.cdt.dsf.debug.service.BreakpointsMediator.ITargetBreakpointInfo;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Enhanced breakpoint attribute translator interface
 * 
 * @since 2.1
 */

public interface IBreakpointAttributeTranslatorExtension extends IBreakpointAttributeTranslator {
	/**
	 * Resolve the breakpoint in given context. A platform BP may be mapped to
	 * two or more target BPs, e.g. a breakpoint in an in-line function may be
	 * mapped to several target BPs, or a thread-specific BP may be mapped to
	 * several target BPs each of which is for one thread. This method will get
	 * the list of attribute maps each of which corresponds to one target BP.<br>
	 * <br>
	 * This method is and must be called in DSF execution thread.
	 * 
	 * @param context
	 *            - a IBreakpointsTargetDMContext object (which could be a
	 *            process or a loaded module) in which we locate target BPs for
	 *            the platform BP. Cannot be null.
	 * @param breakpoint
	 *            - platform breakpoint.
	 * @param bpAttributes
	 *            - all attributes of the breakpoint, usually output from
	 *            {@link #getAllBreakpointAttributes(IBreakpoint, boolean)}.
	 * @param drm
	 *            - on completion of the request, the DataRequestMonitor
	 *            contains one or more attribute maps each of which
	 *            corresponding to one target breakpoint.
	 * @throws CoreException
	 */
    public void resolveBreakpoint(IBreakpointsTargetDMContext context, IBreakpoint breakpoint, 
    		Map<String, Object> bpAttributes, DataRequestMonitor<List<Map<String, Object>>> drm);

	/**
	 * Get all platform defined attributes for a breakpoint plus all attributes
	 * defined by any breakpoint extension such as ICBreakpointExtension in CDT.
	 * In other words, get all attributes available from UI. <br>
	 * <br>
	 * The attributes returned are independent of runtime context (process,
	 * module, etc), whereas attributes from
	 * {@link #resolveBreakpoint(IBreakpointsTargetDMContext, IBreakpoint, Map, DataRequestMonitor)}
	 * are context sensitive. <br>
	 * <br>
	 * Note this method must not be called in DSF dispatch thread because we are
	 * accessing the resources system to retrieve the breakpoint attributes.
	 * Accessing the resources system potentially requires using global locks.
	 * 
	 * @param platformBP
	 * @param bpManagerEnabled
	 * @return list of target (debugger implementation) recognizable attributes.
	 * @throws CoreException
	 */
    public Map<String, Object> getAllBreakpointAttributes(IBreakpoint platformBP, boolean bpManagerEnabled) throws CoreException;

	/**
	 * Convert platform breakpoint attributes to target attributes. This usually
	 * involves changing attributes keys to target recognizable ones. For
	 * instance, GDB integration has its own breakpoint attribute keys. <br>
	 * 
	 * @param platformBPAttr
	 * @return
	 */
    public Map<String, Object> convertAttributes(Map<String, Object> platformBPAttr);
        
    /**
     * Update platform about breakpoint status change, e.g. breakpoint installed on target successfully or breakpoint
     * removed from target successfully.<br>
     * <br>
     * Note this method is not and must not be called in DSF dispatch thread.
     * 
     * @param bpsInfo
     * @param eventType
     */
    public void updateBreakpointsStatus(Map<IBreakpoint, Map<IBreakpointsTargetDMContext, ITargetBreakpointInfo[]>> bpsInfo, BreakpointEventType eventType);

	/**
	 * This is enhanced version of
	 * {@link #canUpdateAttributes(org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointDMContext, Map)}
	 * in that this API gives more context parameters so that client can make
	 * decision on a finer granularity when needed.<br>
	 * <br>
	 * This will be called in DSF dispatch thread.
	 * 
	 * @param bp  platform breakpoint.
	 * @param context 
	 * @param attributes target-recognizable attributes.
	 * @return false as long as one of the attributes cannot be updated by client, otherwise true.
	 */
	public boolean canUpdateAttributes(IBreakpoint bp, IBreakpointsTargetDMContext context, Map<String, Object> attributes);
}