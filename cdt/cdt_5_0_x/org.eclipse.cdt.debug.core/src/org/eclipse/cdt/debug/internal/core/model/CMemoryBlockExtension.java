/*******************************************************************************
 * Copyright (c) 2004, 2007 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.core.model; 

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEventListener;
import org.eclipse.cdt.debug.core.cdi.event.ICDIMemoryChangedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIRestartedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIResumedEvent;
import org.eclipse.cdt.debug.core.cdi.model.ICDIMemoryBlock;
import org.eclipse.cdt.debug.core.cdi.model.ICDIMemoryBlockManagement2;
import org.eclipse.cdt.debug.core.cdi.model.ICDIMemorySpaceManagement;
import org.eclipse.cdt.debug.core.cdi.model.ICDIObject;
import org.eclipse.cdt.debug.core.cdi.model.ICDITarget;
import org.eclipse.cdt.debug.core.model.IExecFileInfo;
import org.eclipse.cdt.debug.internal.core.CMemoryBlockRetrievalExtension;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * Represents a memory block in the CDI model.
 */
public class CMemoryBlockExtension extends CDebugElement implements IMemoryBlockExtension, ICDIEventListener {

	/**
	 * The address expression this memory block is based on.
	 */
	private String fExpression;

	/**
	 * The base address of this memory block.
	 */
	private BigInteger fBaseAddress;

	/**
	 * The memory space identifier; will be null for backends that
	 * don't require memory space support
	 */
	private String fMemorySpaceID;

	/**
	 * The underlying CDI memory block.
	 */
	private ICDIMemoryBlock fCDIBlock;

	/**
	 * The memory bytes values.
	 */
	private MemoryByte[] fBytes = null;

	private HashSet fChanges = new HashSet();

	/**
	 * is fWordSize available?
	 */
	private boolean fHaveWordSize;

	/**
	 * The number of bytes per address.
	 */
	private int fWordSize;


	/** 
	 * Constructor for CMemoryBlockExtension. 
	 */
	public CMemoryBlockExtension( CDebugTarget target, String expression, BigInteger baseAddress ) {
		super( target );
		
		fExpression = expression;
		fBaseAddress = baseAddress;
	}

	/** 
	 * Constructor for CMemoryBlockExtension. 
	 */
	public CMemoryBlockExtension( CDebugTarget target, String expression, BigInteger baseAddress, int wordSize ) {
		super( target );
		fExpression = expression;
		fBaseAddress = baseAddress;
		fWordSize= wordSize;
		fHaveWordSize= true;
	}

	/** 
	 * Constructor for CMemoryBlockExtension that supports memory spaces
	 *  
	 */
	public CMemoryBlockExtension( CDebugTarget target, BigInteger baseAddress, String memorySpaceID ) {
		super( target );
		fBaseAddress = baseAddress;
		fMemorySpaceID = memorySpaceID; 
		if (target.getCDITarget() instanceof ICDIMemorySpaceManagement)
			fExpression = ((ICDIMemorySpaceManagement)target.getCDITarget()).addressToString(baseAddress, memorySpaceID);
		
		if (fExpression == null)
			// If the backend supports memory spaces, it should implement ICDIMemorySpaceManagement
			// Even if it does, it may choose to use our built-in encoding/decoding
			fExpression = CMemoryBlockRetrievalExtension.addressToString(baseAddress, memorySpaceID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getExpression()
	 */
	public String getExpression() {
		return fExpression;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getBigBaseAddress()
	 */
	public BigInteger getBigBaseAddress() {
		return fBaseAddress;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getAddressSize()
	 */
	public int getAddressSize() {
		return ((CDebugTarget)getDebugTarget()).getAddressFactory().createAddress( getBigBaseAddress() ).getSize();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getAddressableSize()
	 */
	public int getAddressableSize() throws DebugException {
		if (!fHaveWordSize)	{
			synchronized (this)	{
				if (!fHaveWordSize)	{
					ICDIMemoryBlock block= getCDIBlock();
					if (block == null) {
						try {
							// create a CDI block of an arbitrary size so we can call into
							// the backend to determine the memory's addressable size
							setCDIBlock( block= createCDIBlock( fBaseAddress, 100 ));
						}
						catch( CDIException e ) {
							targetRequestFailed( e.getMessage(), null );
						}
					}
					fWordSize= block.getWordSize();
					fHaveWordSize= true;
				}
			}
		}
		return fWordSize;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#supportBaseAddressModification()
	 */
	public boolean supportBaseAddressModification() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#setBaseAddress(java.math.BigInteger)
	 */
	public void setBaseAddress( BigInteger address ) throws DebugException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getBytesFromOffset(java.math.BigInteger, long)
	 */
	public MemoryByte[] getBytesFromOffset( BigInteger unitOffset, long addressableUnits ) throws DebugException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getBytesFromAddress(java.math.BigInteger, long)
	 */
	public MemoryByte[] getBytesFromAddress( BigInteger address, long length ) throws DebugException {
		ICDIMemoryBlock cdiBlock = getCDIBlock();
		if ( fBytes == null || cdiBlock == null || 
			 cdiBlock.getStartAddress().compareTo( address ) > 0 || 
			 cdiBlock.getStartAddress().add( BigInteger.valueOf( cdiBlock.getLength()/cdiBlock.getWordSize() ) ).compareTo( address.add( BigInteger.valueOf( length ) ) ) < 0 ) {
			synchronized( this ) {
				byte[] bytes = null;
				try {
					cdiBlock = getCDIBlock();
					if ( cdiBlock == null || 
						 cdiBlock.getStartAddress().compareTo( address ) > 0 || 
						 cdiBlock.getStartAddress().add( BigInteger.valueOf( cdiBlock.getLength()/cdiBlock.getWordSize() ) ).compareTo( address.add( BigInteger.valueOf( length ) ) ) < 0 ) {
						if ( cdiBlock != null ) {
							disposeCDIBlock();
							fBytes = null;
						}
						setCDIBlock( cdiBlock = createCDIBlock( address, length ) );
					}
					bytes = getCDIBlock().getBytes();
				}
				catch( CDIException e ) {
					targetRequestFailed( e.getMessage(), null );
				}
				fBytes = new MemoryByte[bytes.length];
				for ( int i = 0; i < bytes.length; ++i ) {
					fBytes[i] = createMemoryByte( bytes[i], getCDIBlock().getFlags( i ), hasChanged( getRealBlockAddress().add( BigInteger.valueOf( i ) ) ) );
				}
			}
		}
		MemoryByte[] result = new MemoryByte[0];
		if ( fBytes != null ) {
			int offset = address.subtract( getRealBlockAddress() ).intValue();
			int offsetInBytes = offset * cdiBlock.getWordSize();
			long lengthInBytes = length * cdiBlock.getWordSize();
			if ( offset >= 0 ) {
				int size = ( fBytes.length - offsetInBytes >= lengthInBytes ) ? (int)lengthInBytes : fBytes.length - offsetInBytes;
				if ( size > 0 ) {
					result = new MemoryByte[size];
					System.arraycopy( fBytes, offsetInBytes, result, 0, size );
				}
			}
		}
		return result;
	}

	private boolean isBigEndian() {
		IExecFileInfo info = (IExecFileInfo)getDebugTarget().getAdapter( IExecFileInfo.class );
		if ( info != null ) {
			return !info.isLittleEndian();
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getMemoryBlockRetrieval()
	 */
	public IMemoryBlockRetrieval getMemoryBlockRetrieval() {
		return (IMemoryBlockRetrieval)getDebugTarget().getAdapter( IMemoryBlockRetrieval.class );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.cdi.event.ICDIEventListener#handleDebugEvents(org.eclipse.cdt.debug.core.cdi.event.ICDIEvent[])
	 */
	public void handleDebugEvents( ICDIEvent[] events ) {
		for( int i = 0; i < events.length; i++ ) {
			ICDIEvent event = events[i];
			ICDIObject source = event.getSource();
			if ( source == null )
				continue;
			if ( source.getTarget().equals( getCDITarget() ) ) {
				if ( event instanceof ICDIResumedEvent || event instanceof ICDIRestartedEvent ) {
					resetChanges();
				}
				else if ( event instanceof ICDIMemoryChangedEvent ) {
					if ( source instanceof ICDIMemoryBlock && source.equals( getCDIBlock() ) ) {
						handleChangedEvent( (ICDIMemoryChangedEvent)event );
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getStartAddress()
	 */
	public long getStartAddress() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getLength()
	 */
	public long getLength() {
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlock#getBytes()
	 */
	public byte[] getBytes() throws DebugException {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlock#supportsValueModification()
	 */
	public boolean supportsValueModification() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlock#setValue(long, byte[])
	 */
	public void setValue( long offset, byte[] bytes ) throws DebugException {
		setValue( BigInteger.valueOf( offset ), bytes );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#setValue(java.math.BigInteger, byte[])
	 */
	public void setValue( BigInteger offset, byte[] bytes ) throws DebugException {
		ICDIMemoryBlock block = getCDIBlock();
		if ( block != null ) {
			BigInteger base = getBigBaseAddress();
			BigInteger real = getRealBlockAddress();
			long realOffset = base.add( offset ).subtract( real ).longValue();
			try {
				block.setValue( realOffset, bytes );
			}
			catch( CDIException e ) {
				targetRequestFailed( e.getDetailMessage(), null );
			}
		}
	}

	private ICDIMemoryBlock createCDIBlock( BigInteger address, long length) throws CDIException {
		ICDIMemoryBlock block = null;
		CDebugTarget target = (CDebugTarget)getDebugTarget();
		ICDITarget cdiTarget = target.getCDITarget();
		if ((fMemorySpaceID != null) && (cdiTarget instanceof ICDIMemoryBlockManagement2)) {
			block = ((ICDIMemoryBlockManagement2)cdiTarget).createMemoryBlock(address, fMemorySpaceID, (int)length); 
		} else {
			// Note that CDI clients should ignore the word size 
			// parameter. It has been deprecated in 4.0. We continue to
			// pass in 1 as has always been the case to maintain backwards
			// compatibility.
			block = cdiTarget.createMemoryBlock( address.toString(), (int)length, 1);
		}
		block.setFrozen( false );
		getCDISession().getEventManager().addEventListener( this );
		return block;
	}

	private void disposeCDIBlock() {
		ICDIMemoryBlock block = getCDIBlock();
		if ( block != null ) {
			try {
				((CDebugTarget)getDebugTarget()).getCDITarget().removeBlocks( new ICDIMemoryBlock[]{ block  });
			}
			catch( CDIException e ) {
				DebugPlugin.log( e );
			}
			setCDIBlock( null );
			getCDISession().getEventManager().removeEventListener( this );
		}
	}

	private ICDIMemoryBlock getCDIBlock() {
		return fCDIBlock;
	}

	private void setCDIBlock( ICDIMemoryBlock cdiBlock ) {
		fCDIBlock = cdiBlock;
	}

	private BigInteger getRealBlockAddress() {
		ICDIMemoryBlock block = getCDIBlock();
		return ( block != null ) ? block.getStartAddress() : BigInteger.ZERO;
	}

	private long getBlockSize() {
		ICDIMemoryBlock block = getCDIBlock();
		return ( block != null ) ? block.getLength() : 0;
	}

	private void handleChangedEvent( ICDIMemoryChangedEvent event ) {
		ICDIMemoryBlock block = getCDIBlock();
		if ( block != null && fBytes != null ) {
			MemoryByte[] memBytes = (MemoryByte[])fBytes.clone();
			try {
				BigInteger start = getRealBlockAddress();
				long length = block.getLength();
				byte[] newBytes = block.getBytes();
				BigInteger[] addresses = event.getAddresses();
				saveChanges( addresses );
				for ( int i = 0; i < addresses.length; ++i ) {
					fChanges.add( addresses[i] );
					int addressableSize = fCDIBlock.getWordSize(); // # of bytes per address
					if ( addresses[i].compareTo( start ) >= 0 && addresses[i].compareTo( start.add( BigInteger.valueOf( length / addressableSize ) ) ) < 0 ) {
						int index = addressableSize * addresses[i].subtract( start ).intValue();
						int end = Math.min(Math.min(index + addressableSize, memBytes.length), newBytes.length);
						for (index = Math.max(index, 0) ; index < end; index++ ) {
							memBytes[index].setChanged( true );
							memBytes[index].setValue( newBytes[index] );
						}
					}
				}
				fBytes = memBytes;
				fireChangeEvent( DebugEvent.CONTENT );
			}
			catch( CDIException e ) {
				DebugPlugin.log( e );
			}			
		}
	}

	private void saveChanges( BigInteger[] addresses ) {
		fChanges.addAll( Arrays.asList( addresses ) );
	}

	private boolean hasChanged( BigInteger address ) {
		return fChanges.contains( address );
	}

	private void resetChanges() {
		if ( fBytes != null ) {
			BigInteger[] changes = (BigInteger[])fChanges.toArray( new BigInteger[fChanges.size()] );
			for ( int i = 0; i < changes.length; ++i ) {
				BigInteger real = getRealBlockAddress();
				if ( real.compareTo( changes[i] ) <= 0 && real.add( BigInteger.valueOf( getBlockSize() ) ).compareTo( changes[i] ) > 0 ) {
					int index = changes[i].subtract( real ).intValue();
					if ( index >= 0 && index < fBytes.length ) {
						fBytes[index].setChanged( false ); 
					}
				}
			}
		}
		fChanges.clear();
		fireChangeEvent( DebugEvent.CONTENT );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#supportsChangeManagement()
	 */
	public boolean supportsChangeManagement() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#connect(java.lang.Object)
	 */
	public void connect( Object object ) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#disconnect(java.lang.Object)
	 */
	public void disconnect( Object object ) {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getConnections()
	 */
	public Object[] getConnections() {
		// TODO Auto-generated method stub
		return new Object[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#dispose()
	 */
	public void dispose() {
		fChanges.clear();
		ICDIMemoryBlock cdiBlock = getCDIBlock();
		if ( cdiBlock != null ) {
			try {
				((CDebugTarget)getDebugTarget()).getCDITarget().removeBlocks( new ICDIMemoryBlock[] {cdiBlock} );
			}
			catch( CDIException e ) {
				CDebugCorePlugin.log( e );
			}
			fCDIBlock = null;
		}
		getCDISession().getEventManager().removeEventListener( this );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter( Class adapter ) {
		if ( IMemoryBlockRetrieval.class.equals( adapter ) )
			return getMemoryBlockRetrieval();
		return super.getAdapter( adapter );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getMemoryBlockStartAddress()
	 */
	public BigInteger getMemoryBlockStartAddress() throws DebugException {
		return null; // return null to mean not bounded ... according to the spec
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getMemoryBlockEndAddress()
	 */
	public BigInteger getMemoryBlockEndAddress() throws DebugException {
		return null;// return null to mean not bounded ... according to the spec
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockExtension#getBigLength()
	 */
	public BigInteger getBigLength() throws DebugException {
		ICDIMemoryBlock block = getCDIBlock();
		if ( block != null ) {
			BigInteger length = new BigInteger( Long.toHexString( block.getLength() ), 16 );
			return length;
		}
		return BigInteger.ZERO;
	}

	private MemoryByte createMemoryByte( byte value, byte cdiFlags, boolean changed ) {
		byte flags = 0;
		if ( (cdiFlags & ICDIMemoryBlock.VALID) != 0 ) {
			flags |= MemoryByte.HISTORY_KNOWN | MemoryByte.ENDIANESS_KNOWN;
			if ( (cdiFlags & ICDIMemoryBlock.READ_ONLY) != 0 ) {
				flags |= MemoryByte.READABLE;
			}
			else {
				flags |= MemoryByte.READABLE | MemoryByte.WRITABLE;
			}
			if ( isBigEndian() ) {
				flags |= MemoryByte.BIG_ENDIAN;
			}
			if ( changed )
				flags |= MemoryByte.CHANGED;
		}
		return new MemoryByte( value, flags );
	}

	
	/**
	 * Provides the memory space associated with this block if and only if the 
	 * block was created with an address value + memory space qualifier. If the 
	 * block was created from an expression, this method should return null--
	 * even if the target CDI backend supports memory spaces. 
	 * 
	 * @return a memory space ID or null
	 * expression
	 */
	public String getMemorySpaceID() {
		return fMemorySpaceID;
	}
}
