/*******************************************************************************
 * Copyright (c) 2006, 2008 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.ui.viewmodel;

import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.service.IDsfService;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.AbstractDMVMProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenCountUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IHasChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;

/**
 * View model nodes are configured with a view model provider to collectively 
 * define the layout of a view.  Each layout node generates elements of type 
 * {@link IVMContext} which are then stored in the viewer.
 * 
 * <p/>
 * NOTE: This interface extends <code>IElementContentProvider</code> but it has 
 * slightly different parameter requirements.  For the 
 * {@link IElementContentProvider#update(IChildrenUpdate[])} method, this class 
 * can accept an update where {@link IChildrenUpdate#getOffset()}  and 
 * {@link IChildrenUpdate#getLength()} may return -1. In this case the 
 * implementation should return all available elements for the given parent.<br>
 * Also the for the {@link IElementContentProvider#update(IHasChildrenUpdate[])} and 
 * {@link IElementContentProvider#update(IChildrenCountUpdate[])} methods, the 
 * implementation may return an error with an error code of {@link IDsfService#NOT_SUPPORTED}.  
 * In this case the caller of this update should call 
 * {@link IElementContentProvider#update(IChildrenUpdate[])}
 * instead.
 * 
 * @see AbstractDMVMProvider
 * @see IElementContentProvider
 * 
 * @since 1.0
 */
@ConfinedToDsfExecutor("")
public interface IVMNode extends IElementContentProvider
{
    /**
     * Retrieves the view model provider that this node is configured with.
     */
    public IVMProvider getVMProvider();
    
    /**
     * Returns the potential delta flags that would be generated by this node 
     * for the given event.  
     * @param event Event to process.
     * @return IModelDelta flags
     * @see #buildDelta(Object, VMDelta, int, RequestMonitor)
     * @see IModelDelta
     */
    public int getDeltaFlags(Object event);
    
    /**
     * Builds model delta information based on the given event.  
     * <p>
     * Model deltas, which are used to control the state of elements in the viewer, are 
     * generated by the layout nodes by recursively calling this method on all the nodes 
     * in the layout tree.  Each node implements two methods: {@link #getDeltaFlags(Object)}, 
     * and <code>buildDelta()</code>.  A parent node which is processing a 
     * <code>buildDelta</code> operation needs to determine which of its elements are
     * affected by a given event, set appropriate flags on these elements, and then 
     * it needs to call its child nodes with those elements to give the child nodes a 
     * chance to add onto the delta.  
     * </p>
     * <p>
     * The <code>getDeltaFlags()</code> is a synchronous
     * call which tells the parent node whether on not to call the child node's 
     * <code>buildDelta</code> with the given event.  If a child node return 
     * <code>true</code>, it only indicates that the node may add delta flags, but it
     * does not require it to do so.
     * </p>  
     * 
     * @param event Event to process.
     * @param parent Parent model delta node that this object should add delta
     * data to.
     * @param nodeOffset The offset of the first element in this node.  This offset
     * depends on the elements returned by the siblings of this layout node.
     * @param requestMonitor Return token, which notifies the caller that the calculation is
     * complete.
     */
    public void buildDelta(Object event, VMDelta parent, int nodeOffset, RequestMonitor requestMonitor);

    /**
     * Retireves the view model elements for the given data model event.  This method 
     * is optional and it allows the view model provider to optimize event processing
     * by avoiding the need to retrieve all possible elements for the given node.
     * </p>
     * For example:  If a threads node implementation is given a thread stopped event in 
     * this method, and the stopped event included a reference to the thread.  Then
     * the implementation should create a view model context for that thread and return it
     * here.
     *   
     * @param parentDelta The parent delta in the processing of this event.
     * @param event The event to check for the data model object.
     * @param Request monitor for the array of elements corresponding to the 
     * given event.
     */
    public void getContextsForEvent(VMDelta parentDelta, Object event, DataRequestMonitor<IVMContext[]> rm);

    /**
     * Releases the resources held by this node.
     */
    public void dispose();
}