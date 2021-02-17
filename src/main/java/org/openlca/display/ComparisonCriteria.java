package org.openlca.display;

public enum ComparisonCriteria {
	LOCATION("Location"), AMOUNT("Amount"), CATEGORY("Category");

	private String criteria;

	ComparisonCriteria(String c) {
		criteria = c;
	}
	
	public static ComparisonCriteria getCriteria(String c) {
		for (ComparisonCriteria comparisonCriteria : values()) {
			if(comparisonCriteria.criteria.equals(c)) {
				return comparisonCriteria;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return criteria;
	}
}
