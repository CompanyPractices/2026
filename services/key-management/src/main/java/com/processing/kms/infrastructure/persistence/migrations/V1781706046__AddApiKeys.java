package com.processing.kms.infrastructure.persistence.migrations;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;

public class V1781706046__AddApiKeys extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        try (Statement sql = context.getConnection().createStatement()) {
            sql.execute("""
            create type api_key_role as enum ('READ', 'WRITE', 'ADMIN');

            create table api_keys (
                key        varchar(256) primary key,
                owner_id   varchar(256) not null,
                role       api_key_role not null,
                issued_at  timestamp with time zone not null default now(),
                expires_by timestamp with time zone not null default now(),
                is_expired boolean not null default false
            );
            """);
        }
    }
}
