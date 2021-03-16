package org.openlca.display;

public enum TargetEnum {
	IMPACT("Impact categories"), PRODUCT("Product systems");

	private String criteria;

	TargetEnum(String c) {
		criteria = c;
	}

	public static TargetEnum getTarget(String c) {
		for (TargetEnum comparisonCriteria : values()) {
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
