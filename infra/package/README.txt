Default cache size for db in standalone, has been set to 128M,
if you need to lower that, you'll need to edit nis/db.properties

For NIS we've also added initial and max memory that java process can get.
You can modify this using the switches:
-Xms4G
-Xmx6G
