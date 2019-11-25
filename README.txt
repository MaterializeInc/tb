1. set the WAL level to logical

psql> ALTER SYSTEM set wal_level to logical;
bash> brew services restart postgres
psql> SHOW wal_level;

2. 