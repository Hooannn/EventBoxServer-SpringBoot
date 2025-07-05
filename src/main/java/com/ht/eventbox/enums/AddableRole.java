package com.ht.eventbox.enums;

public enum AddableRole {
    STAFF, MANAGER;

    public OrganizationRole toOrganizationRole() {
        return switch (this) {
            case STAFF -> OrganizationRole.STAFF;
            case MANAGER -> OrganizationRole.MANAGER;
        };
    }
}
