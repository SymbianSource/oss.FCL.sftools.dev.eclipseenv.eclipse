/*******************************************************************************
 * Copyright (c) 2002, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.parser.token;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.core.parser.ITokenDuple;
import org.eclipse.cdt.core.parser.util.CharArrayUtils;

/**
 * @author jcamelon
 *
 */
public class BasicTokenDuple implements ITokenDuple {

	BasicTokenDuple( IToken first, IToken last )
	{
//		assert ( first != null && last != null ) : this; 
		firstToken = first; 
		lastToken = last; 
	}
	
	protected int numSegments = -1;
	
	BasicTokenDuple( ITokenDuple firstDuple, ITokenDuple secondDuple ){
		this( firstDuple.getFirstToken(), secondDuple.getLastToken() );
	}
	
	protected final IToken firstToken, lastToken;

	
	public IToken getFirstToken() {
		return firstToken;
	}

	public IToken getLastToken() {
		return lastToken;
	}
	
	public Iterator<IToken> iterator()
	{
		return new TokenIterator(); 
	}
	
	
	public ITokenDuple getLastSegment()
	{
		IToken first = null, last = null, token = null;
		for( ; ; ){
		    if( token == getLastToken() )
		        break;
			token = ( token != null ) ? token.getNext() : getFirstToken();
			if( first == null )
				first = token;
			if( token.getType() == IToken.tLT )
				token = TokenFactory.consumeTemplateIdArguments( token, getLastToken() );
			else if( token.getType() == IToken.tCOLONCOLON ){
				first = null;
				continue;
			}
			last = token;
		}
		
		List<IASTNode> [] args = getTemplateIdArgLists();
		if( args != null && args[ args.length - 1 ] != null ){
			List<List<IASTNode>> newArgs = new ArrayList<List<IASTNode>>( 1 );
			newArgs.add( args[ args.length - 1 ] );
			return TokenFactory.createTokenDuple( first, last, newArgs );
		} 
		return TokenFactory.createTokenDuple( first, last );
	}

	public ITokenDuple[] getSegments()
	{
		
		List<ITokenDuple> r = new ArrayList<ITokenDuple>();
		IToken token = null;
		IToken prev = null;
		IToken last = getLastToken();
		IToken startOfSegment = getFirstToken();
		for( ;; ){
		    if( token == last )
		        break;
		    if( startOfSegment == last.getNext() && startOfSegment.getType() != IToken.tEOC ) // for the EOC token, next points to itself
		    {
		    	startOfSegment = null;
		    	break;
		    }
		    prev = token;
			token = ( token != null ) ? token.getNext() : getFirstToken();
			if( token.getType() == IToken.tLT )
				token = TokenFactory.consumeTemplateIdArguments( token, last );
			if( token.getType() == IToken.tCOLONCOLON  ){
				if( startOfSegment == token ){
					//an empty segment, prev is not valid (and neither is the code)
					prev = null;
				}
			    ITokenDuple d = TokenFactory.createTokenDuple( startOfSegment, ( prev == null ) ? startOfSegment : prev );
			    r.add( d );
			    startOfSegment = token.getNext();
				continue;
			}
		}
		if( startOfSegment != null )
		{
			ITokenDuple d = TokenFactory.createTokenDuple( startOfSegment, last );
			r.add( d );
		}
		return r.toArray( new ITokenDuple[ r.size() ]);

	}
	
	public ITokenDuple getLeadingSegments(){
		if( getFirstToken() == null )
			return null;
		
		int num = getSegmentCount();
		
		if( num <= 1 )
			return null;
		
		IToken first = null, last = null;
		IToken previous = null, token = null;

		for( ; ; ){
		    if( token == getLastToken() )
		        break;
			token = ( token != null ) ? token.getNext() : getFirstToken();
			if( first == null )
				first = token;
			if( token.getType() == IToken.tLT )
				token = TokenFactory.consumeTemplateIdArguments( token, getLastToken() );
			else if( token.getType() == IToken.tCOLONCOLON ){
				last = previous;
				continue;
			}
			previous = token;
		}
		
		if( last == null ){
			//"::A"
			return null;
		}
		
		if( getTemplateIdArgLists() != null ){
			List<IASTNode>[] args = getTemplateIdArgLists();
			List<List<IASTNode>> newArgs = new ArrayList<List<IASTNode>>( args.length - 1 );
			boolean foundArgs = false;
			for( int i = 0; i < args.length - 1; i++ ){
				newArgs.add( args[i] );
				if( args[i] != null ) 
					foundArgs = true;
			}
			return TokenFactory.createTokenDuple( first, last, ( foundArgs ? newArgs : null ) );
		} 
		return TokenFactory.createTokenDuple( first, last );
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#getSegmentCount()
	 */
	public int getSegmentCount() {
		if( numSegments == -1 )
			numSegments = calculateSegmentCount();
		return numSegments;
	}	
	
	private static final char[] EMPTY_STRING = "".toCharArray(); //$NON-NLS-1$
	private char[] stringRepresentation = null;
	
	private class TokenIterator implements Iterator<IToken>
	{
		private IToken iter = firstToken;

		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return ( iter != null );
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public IToken next() {
			if( ! hasNext() )
				throw new NoSuchElementException();
			IToken temp = iter;
			if( iter == lastToken )
				iter = null; 
			else
				iter = iter.getNext();
			return temp;
		}

		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException(); 			
		}
		
	}

	public static int getCharArrayLength( IToken f, IToken l ){
		if( f == l )
			return f.getCharImage().length;
		
		IToken prev = null;
		IToken iter = f;
		
		int length = 0;
		for( ; ; ){
			if( iter == null ) return 0;
			if( prev != null && prev.getType() != IToken.tCOLONCOLON && 
								prev.getType() != IToken.tIDENTIFIER && 
								prev.getType() != IToken.tLT &&
								prev.getType() != IToken.tBITCOMPLEMENT &&
								iter.getType() != IToken.tGT && 
								prev.getType() != IToken.tLBRACKET && 
								iter.getType() != IToken.tRBRACKET && 
								iter.getType() != IToken.tCOLONCOLON )
			{
				length++;
			}
			length += iter.getCharImage().length;
			if( iter == l ) break;
			prev = iter;
			iter = iter.getNext();
		}
		return length;
	}
	
	public static char[] createCharArrayRepresentation( IToken f, IToken l)
	{
		if( f == l ) return f.getCharImage();
		
		IToken prev = null;
		IToken iter = f;
		
		int length = getCharArrayLength( f, l );
		
		char[] buff = new char[ length ];

		for( int i = 0; i < length; )
		{
			if( prev != null && 
			    prev.getType() != IToken.tCOLONCOLON && 
				prev.getType() != IToken.tIDENTIFIER && 
				prev.getType() != IToken.tLT &&
				prev.getType() != IToken.tBITCOMPLEMENT &&
				iter.getType() != IToken.tGT && 
				prev.getType() != IToken.tLBRACKET && 
				iter.getType() != IToken.tRBRACKET && 
				iter.getType() != IToken.tCOLONCOLON )
				buff[i++] = ' ';
			
			if( iter == null ) return EMPTY_STRING;
			CharArrayUtils.overWrite( buff, i, iter.getCharImage() );
			i+= iter.getCharImage().length;
			if( iter == l ) break;
			prev = iter;
			iter = iter.getNext();
		}
		return buff;
		
	}
	
	@Override
	public String toString() 
	{
		if( stringRepresentation == null )
			stringRepresentation = createCharArrayRepresentation(firstToken, lastToken);
		return String.valueOf(stringRepresentation);
	}
	
    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.parser.ITokenDuple#length()
     */
    public int length()
    {
        int count = 1; 
        IToken i = firstToken;
        while( i != lastToken )
        {
        	++count;
        	i = i.getNext();
        }
        return count;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.parser.ITokenDuple#getSubDuple(int, int)
     */
    public ITokenDuple getSubrange(int startIndex, int endIndex)
    {
		return TokenFactory.createTokenDuple( getToken( startIndex ), getToken( endIndex) );
    }

    public IToken getToken(int index)
    {
        if( index < 0 ) return null;
        
        IToken iter = firstToken;
        int count = 0;
        while( iter != lastToken )
        {
        	iter = iter.getNext();
        	if( count == index )
        		return iter;
        	++count; 
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.parser.ITokenDuple#findLastTokenType(int)
     */
    public int findLastTokenType(int type)
    {
		int count = 0; 
		int lastFound = -1;
        IToken i = firstToken;
        while( i != lastToken )
        {
        	if( i.getType() == type )
        		lastFound = count; 
        	++count;
        	i = i.getNext();
        }
        
        return lastFound;
    }
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#getEndOffset()
	 */
	public int getEndOffset() {
		return getLastToken().getEndOffset();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#getStartOffset()
	 */
	public int getStartOffset() {
		return getFirstToken().getOffset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#getTemplateIdArgLists()
	 */
	public List<IASTNode>[] getTemplateIdArgLists() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#syntaxOfName()
	 */
	public boolean syntaxOfName() {
		IToken iter = firstToken;
		while( iter != lastToken)
		{
		    if( iter.getType() == IToken.tLT ){
				iter = TokenFactory.consumeTemplateIdArguments( iter, lastToken );
				if( iter.getType() == IToken.tGT ){
				    if( iter == lastToken ) break;
				    iter = iter.getNext();
				    continue;
				}
				continue;
		    }
		    
			if( iter.isOperator() )
			{
				iter = iter.getNext();
				continue;
			}
			switch( iter.getType() )
			{
				case IToken.tBITCOMPLEMENT:
				case IToken.tIDENTIFIER:
				case IToken.tCOLONCOLON:
				case IToken.t_operator:
					iter = iter.getNext();
					continue;
				default:
					return false;
			}
		}
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if( !(other instanceof ITokenDuple ) ) return false;
		if( ((ITokenDuple) other).getFirstToken().equals( getFirstToken() ) &&
			((ITokenDuple) other).getLastToken().equals( getLastToken() ) )
			return true;
		return false;
	}
	
	public ITokenDuple getTemplateIdNameTokenDuple() {
	 	ITokenDuple nameDuple = getLastSegment(); 
	 	
	    List<IASTNode>[] argLists = getTemplateIdArgLists(); 
	    if( argLists == null || argLists[ argLists.length - 1 ] == null )
	        return nameDuple;
	 	
	    IToken i = nameDuple.getFirstToken();
	    IToken last = nameDuple.getLastToken();
    	
    	if( i.getType() == IToken.t_template )
    		i = i.getNext();
    	
    	if( i == last )
    		return TokenFactory.createTokenDuple(i, i);
    	
    	IToken first= i;
    	    	
    	//destructors
    	if( i.getType() == IToken.tBITCOMPLEMENT ){
    		i = i.getNext();
    	} 
    	//operators
    	else if( i.getType() == IToken.t_operator ){
    		i =  i.getNext();
		    IToken temp = null;
		    while( i != last ){
		        temp = i.getNext();
		        if( temp.getType() != IToken.tLT )
		            i =  temp;
		        else
		            break;
		    }
    	}
    	
		return TokenFactory.createTokenDuple(first, i);
	}

	 public char[] extractNameFromTemplateId(){
	 	ITokenDuple nameDuple = getLastSegment(); 
	 	
	    List<IASTNode>[] argLists = getTemplateIdArgLists(); 
	    if( argLists == null || argLists[ argLists.length - 1 ] == null )
	        return nameDuple.toCharArray();
	 	
	    IToken i = nameDuple.getFirstToken();
	    IToken last = nameDuple.getLastToken();
    	
    	if( i == null )
    		return EMPTY_STRING;
    	else if( i.getType() == IToken.t_template )
    		i = i.getNext();
    	
    	char[] tempArray = i.getCharImage();
    	
    	if( i == last )
    		return tempArray;
    	
    	
    	char[] nameBuffer = new char[ getCharArrayLength( i, lastToken ) ];
    	
    	CharArrayUtils.overWrite( nameBuffer, 0, tempArray );
    	int idx = tempArray.length;
    	
    	//appending of spaces needs to be the same as in toString()
    	    	
    	//destructors
    	if( i.getType() == IToken.tBITCOMPLEMENT ){
    		i = i.getNext();
    		tempArray = i.getCharImage();
    		CharArrayUtils.overWrite( nameBuffer, idx, tempArray );
    		idx += tempArray.length;
    	} 
    	//operators
    	else if( i.getType() == IToken.t_operator ){
    		i =  i.getNext();
    		nameBuffer[ idx++ ] = ' ';
	
		    IToken first = i;
		    IToken temp = null;
		    while( i != last ){
		        temp = i.getNext();
		        if( temp.getType() != IToken.tLT )
		            i =  temp;
		        else
		            break;
		    }
		    CharArrayUtils.overWrite( nameBuffer, idx, createCharArrayRepresentation( first, i ) );
		    idx += getCharArrayLength( first, i );
    	}
    	
    	return CharArrayUtils.extract( nameBuffer, 0, idx );
    }

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#contains(org.eclipse.cdt.core.parser.ITokenDuple)
	 */
	public boolean contains(ITokenDuple duple) {
		if( duple == null ) return false;
		boolean foundFirst = false;
		boolean foundLast = false;
		for( IToken current = getFirstToken(); current != null; current = current.getNext() )
		{
			if( current == duple.getFirstToken() ) foundFirst = true;
			if( current == duple.getLastToken() ) foundLast = true;
			if( foundFirst && foundLast )   break;
			if( current == getLastToken() ) break;
		}

		return ( foundFirst && foundLast );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#toQualifiedName()
	 */
	public String[] toQualifiedName() {
		return generateQualifiedName();
	}

	/**
	 * 
	 */
	private String [] generateQualifiedName() {
		List<String> qn = new ArrayList<String>();
		IToken i = firstToken;
		while( i != lastToken )
		{
			boolean compl = false;
			if( i.getType() == IToken.tCOLONCOLON )
			{
				i = i.getNext();
				continue;
			}
			if( i.getType() == IToken.tBITCOMPLEMENT )
			{
				compl = true;
				i = i.getNext();
			}
			if( i.getType() == IToken.tIDENTIFIER )
			{
				if( compl )
				{
					StringBuffer buff = new StringBuffer( "~" ); //$NON-NLS-1$
					buff.append( i.getImage() );
					qn.add(  buff.toString() ); 
				}
				else
					qn.add( i.getImage() );
			}
			i = i.getNext();
		}
		if( i.getType() == IToken.tIDENTIFIER ){
		    qn.add( i.getImage() );
		}
		String [] qualifiedName = new String[ qn.size() ];
		return qn.toArray( qualifiedName );
	}

	/**
	 * 
	 */
	protected int calculateSegmentCount() {
		int n = 1;
		
		IToken token = null;
		IToken last = getLastToken();
		for( ;; ){
		    if( token == last )
		        break;
			token = ( token != null ) ? token.getNext() : getFirstToken();
			if( token == null ) break;
			if( token.getType() == IToken.tLT )
				token = TokenFactory.consumeTemplateIdArguments( token, last );
			if( token.getType() == IToken.tCOLONCOLON  ){
				n++;
				continue;
			}
		}
		return n;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.ITokenDuple#toCharArray()
	 */
	public char[] toCharArray() {
	    if( stringRepresentation == null )
			stringRepresentation = createCharArrayRepresentation(firstToken, lastToken);
	    return stringRepresentation;
	}
}
