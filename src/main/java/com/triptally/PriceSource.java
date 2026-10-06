package com.triptally;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceSource
{
	GRAND_EXCHANGE("Grand Exchange"),
	HIGH_ALCHEMY("High Alchemy"),
	HIGHEST("Highest of both");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
