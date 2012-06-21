package com.ebay.erl.mobius.core.function;

import java.io.IOException;
import java.util.Comparator;

import com.ebay.erl.mobius.core.function.base.SingleInputAggregateFunction;
import com.ebay.erl.mobius.core.model.Column;
import com.ebay.erl.mobius.core.model.Tuple;
import com.ebay.erl.mobius.core.model.TupleColumnComparator;
import com.ebay.erl.mobius.util.SerializableComparator;
import com.ebay.erl.mobius.util.Util;

/**
 * Calculates the minimum value of the given
 * <code>inputColumn</code> in {@link #Min(Column)}
 * in a group.
 * <p>
 * 
 * By default, {@link TupleColumnComparator} is used to 
 * compare two different values. For different ordering, 
 * use a {@link SerializableComparator}
 * in {@link #Min(Column, SerializableComparator)}.
 * 
 * <p>
 * This product is licensed under the Apache License,  Version 2.0, 
 * available at http://www.apache.org/licenses/LICENSE-2.0.
 * 
 * This product contains portions derived from Apache hadoop which is 
 * licensed under the Apache License, Version 2.0, available at 
 * http://hadoop.apache.org.
 * 
 * � 2007 � 2012 eBay Inc., Evan Chiu, Woody Zhou, Neel Sundaresan
 * 
 */
@SuppressWarnings("unchecked")
public class Min extends SingleInputAggregateFunction 
{	

	private static final long serialVersionUID = 4163999711314409439L;

	/**
	 * The default comparator.
	 * <p>
	 * 
	 * Use TupleColumnComparator so it is possible
	 * to compare values in different but exchangeable
	 * type, ex: comparing integer with double.
	 */
	protected transient TupleColumnComparator _comparator;
	
	/**
	 * the type of the value of the input column
	 */
	protected byte valueType;
	
	/**
	 * the full class name of user specified comparator,
	 * specified in {@link #Min(Column, Comparator)}
	 */
	protected String _user_specified_comparator_clazz = null;
	
	/**
	 * user specified comparator.
	 */
	protected transient Comparator<Object> _user_specified_comparator;
	
	
	/**
	 * Create an instance of {@link Min} operation to
	 * get the minimum value of the given 
	 * <code>inputColumn</code> within a group.
	 * <p>
	 * 
	 * The comparing is natural ordering.
	 */
	public Min(Column inputColumn) 
	{
		super(inputColumn);
	}
	
	
	/**
	 * Create an instance of {@link Min} operation to
	 * get the minimum value of the given 
	 * <code>inputColumn</code> within a group.
	 * <p>
	 * 
	 * The comparing is done by user specified <code>comparator</code>.
	 */
	public Min(Column inputColumn, Comparator<Object> comparator)
	{
		this(inputColumn);
		this._user_specified_comparator_clazz = comparator.getClass().getCanonicalName();
	}

	@Override
	public void consume(Tuple tuple) 
	{
		Object newValue = tuple.get(this.inputColumnName);
		
		if( newValue==null )
			return;
		
		if( this.aggregateResult!=null )
		{
			try 
			{
				int result;
				
				if( this._user_specified_comparator==null )
					result = _comparator.compare(this.aggregateResult, newValue, null);
				else
					result = this._user_specified_comparator.compare(this.aggregateResult, newValue);
				
				if( result>0 )
				{
					// the current min is greater than the new value,
					// replace it
					this.aggregateResult = newValue;
				}
			}
			catch (IOException e)
			{			
				throw new RuntimeException(e);
			}
		}	
		else
		{
			// there is no current min, initialize it and set
			// the value type and comparator
			this.aggregateResult		= newValue;
			this.valueType	= Tuple.getType(this.aggregateResult);
			
			if( _comparator==null && this._user_specified_comparator_clazz==null )
				_comparator = new TupleColumnComparator();
			else if (this._user_specified_comparator_clazz!=null && this._user_specified_comparator==null )
			{
				this._user_specified_comparator = (Comparator)Util.newInstance(this._user_specified_comparator_clazz);
			}
		}
	}
	
	@Override
	public final boolean isCombinable()
	{
		return true;
	}
}