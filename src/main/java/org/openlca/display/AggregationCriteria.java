package org.openlca.display;

public enum AggregationCriteria {
	NONE("None"), CATEGORY("Category"), LOCATION("Location");

	private String criteria;

	AggregationCriteria(String c) {
		criteria = c;
	}

	public static AggregationCriteria getCriteria(String c) {
		for (AggregationCriteria comparisonCriteria : values()) {
			if (comparisonCriteria.criteria.equals(c)) {
				return comparisonCriteria;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return criteria;
	}

	public static String[] valuesToString() {
		var criterias = values();
		String[] crits = new String[criterias.length];
		for (int i = 0; i < criterias.length; i++) {
			crits[i] = criterias[i].toString();
		}
		return crits;
	}
}
