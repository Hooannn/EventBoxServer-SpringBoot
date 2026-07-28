package com.ht.eventbox.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityEqualityTests {

	@Test
	void generatedIdEntitiesWithTheSamePersistedIdAreEqual() {
		assertGeneratedIdEntityEquality(new User(), new User(), new User());
		assertGeneratedIdEntityEquality(new Organization(), new Organization(), new Organization());
		assertGeneratedIdEntityEquality(new Role(), new Role(), new Role());
		assertGeneratedIdEntityEquality(new Permission(), new Permission(), new Permission());
		assertGeneratedIdEntityEquality(new Category(), new Category(), new Category());
		assertGeneratedIdEntityEquality(new Event(), new Event(), new Event());
		assertGeneratedIdEntityEquality(new EventShow(), new EventShow(), new EventShow());
		assertGeneratedIdEntityEquality(new Ticket(), new Ticket(), new Ticket());
		assertGeneratedIdEntityEquality(new TicketItem(), new TicketItem(), new TicketItem());
		assertGeneratedIdEntityEquality(new TicketItemTrace(), new TicketItemTrace(), new TicketItemTrace());
		assertGeneratedIdEntityEquality(new Order(), new Order(), new Order());
		assertGeneratedIdEntityEquality(new Payment(), new Payment(), new Payment());
		assertGeneratedIdEntityEquality(new PaymentSession(), new PaymentSession(), new PaymentSession());
		assertGeneratedIdEntityEquality(new Capture(), new Capture(), new Capture());
		assertGeneratedIdEntityEquality(new Refund(), new Refund(), new Refund());
		assertGeneratedIdEntityEquality(new Voucher(), new Voucher(), new Voucher());
		assertGeneratedIdEntityEquality(new Notification(), new Notification(), new Notification());
		assertGeneratedIdEntityEquality(new FCMToken(), new FCMToken(), new FCMToken());
	}

	@Test
	void naturalIdEntitiesWithTheSamePersistedIdAreEqual() {
		var firstAsset = new Asset();
		firstAsset.setId("asset-1");
		var secondAsset = new Asset();
		secondAsset.setId("asset-1");
		var otherAsset = new Asset();
		otherAsset.setId("asset-2");

		assertThat(firstAsset).isEqualTo(secondAsset);
		assertThat(firstAsset).hasSameHashCodeAs(secondAsset);
		assertThat(firstAsset).isNotEqualTo(otherAsset);

		var firstKeyword = new Keyword();
		firstKeyword.setName("music");
		var secondKeyword = new Keyword();
		secondKeyword.setName("music");
		var otherKeyword = new Keyword();
		otherKeyword.setName("sport");

		assertThat(firstKeyword).isEqualTo(secondKeyword);
		assertThat(firstKeyword).hasSameHashCodeAs(secondKeyword);
		assertThat(firstKeyword).isNotEqualTo(otherKeyword);
	}

	@Test
	void embeddedIdEntityUsesItsCompositeId() {
		var first = new UserOrganization();
		first.setId(UserOrganizationId.builder().userId(1L).organizationId(2L).build());
		var second = new UserOrganization();
		second.setId(UserOrganizationId.builder().userId(1L).organizationId(2L).build());
		var other = new UserOrganization();
		other.setId(UserOrganizationId.builder().userId(1L).organizationId(3L).build());

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(other);
	}

	@Test
	void transientGeneratedIdEntitiesOnlyEqualThemselvesAndKeepStableHashCode() {
		var user = new User();
		var other = new User();
		var hashCodeBeforeId = user.hashCode();

		assertThat(user).isEqualTo(user);
		assertThat(user).isNotEqualTo(other);

		user.setId(1L);

		assertThat(user.hashCode()).isEqualTo(hashCodeBeforeId);
	}

	private void assertGeneratedIdEntityEquality(Object first, Object second, Object other) {
		setId(first, 1L);
		setId(second, 1L);
		setId(other, 2L);

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(other);
	}

	private void setId(Object entity, Long id) {
		try {
			entity.getClass().getMethod("setId", Long.class).invoke(entity, id);
		} catch (ReflectiveOperationException ex) {
			throw new AssertionError("Unable to set entity id for " + entity.getClass().getSimpleName(), ex);
		}
	}
}
