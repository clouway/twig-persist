/**
 *
 */
package com.vercer.engine.persist.festival;

class DanceBand extends Band
{
	int tabletsConfiscated;

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + tabletsConfiscated;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!super.equals(obj))
		{
			return false;
		}
		if (!(obj instanceof DanceBand))
		{
			return false;
		}
		DanceBand other = (DanceBand) obj;
		if (tabletsConfiscated != other.tabletsConfiscated)
		{
			return false;
		}
		return true;
	}
}