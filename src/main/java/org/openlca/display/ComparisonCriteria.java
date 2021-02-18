package org.openlca.display;

public enum ComparisonCriteria {
	AMOUNT("Amount"), CATEGORY("Category"), LOCATION("Location");

	private String criteria;

	ComparisonCriteria(String c) {
		criteria = c;
	}

	public static ComparisonCriteria getCriteria(String c) {
		for (ComparisonCriteria comparisonCriteria : values()) {
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
