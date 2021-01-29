alter table core.edit
    alter column description type text using description::text;