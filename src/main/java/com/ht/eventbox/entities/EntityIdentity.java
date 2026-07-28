package com.ht.eventbox.entities;

import org.hibernate.Hibernate;

import java.util.Objects;

final class EntityIdentity {

	private EntityIdentity() {
	}

	static boolean equals(Object self, Object other, Object selfId, Object otherId) {
		if (self == other) {
			return true;
		}

		if (other == null || Hibernate.getClass(self) != Hibernate.getClass(other)) {
			return false;
		}

		return selfId != null && Objects.equals(selfId, otherId);
	}

	static int hashCode(Object self) {
		return Hibernate.getClass(self).hashCode();
	}
}
