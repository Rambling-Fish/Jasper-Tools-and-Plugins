package com.coralcea.jasper.tools.dta;

public class DTACardinality {

	public static final String ZERO_STAR = "";
	public static final String ZERO_ONE = "[0..1]";
	public static final String ONE_STAR = "[1..*]";
	public static final String ONE_ONE = "[1..1]";
	
	public static String[] toArray() {
		return new String[] {ZERO_STAR, ZERO_ONE, ONE_STAR, ONE_ONE};
	}
}
