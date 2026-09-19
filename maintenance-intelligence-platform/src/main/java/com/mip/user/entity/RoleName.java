package com.mip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleName {

    PLATFORM_ADMIN("Software owner: everything an admin can do, plus the cross-organisation "
            + "oversight console"),
    ADMIN("Full access: user visibility, configuration, all plant data and every action"),
    ENGINEER("Operational access: imports, validation queue, record entry, analytics"),
    VIEWER("Read-only access to machines, records, analytics and insights");

    private final String description;
}
