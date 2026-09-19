package com.mip.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleName {

    ADMIN("Full access: user visibility, configuration, all plant data and every action"),
    ENGINEER("Operational access: imports, validation queue, record entry, analytics"),
    VIEWER("Read-only access to machines, records, analytics and insights");

    private final String description;
}
