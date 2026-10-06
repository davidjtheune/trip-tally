package com.triptally;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HighlightStyle
{
	OUTLINE("Outline"),
	FILL("Fill"),
	BOTH("Outline and fill");

	private final String name;

	@Override
	public String toString()
	{
		return name;
	}
}
