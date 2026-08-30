package com.mihirgamre.taskforge.domain.identity;

public enum OrganizationRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    public boolean canWrite() {
        return this == OWNER || this == ADMIN || this == MEMBER;
    }
}
