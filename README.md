# V-Quiz

Quiz app built with Vaadin and Wildfly.

Steps to play around with this app:

### 1. Add "datasource" to Wildfly

Whatever wildfly config you are using, add an Infinispan cache for this demo. E.g. add following lines to standalone-full-ha.xml, under `<subsystem xmlns="urn:jboss:domain:infinispan:2.0">` section :

```
<cache-container name="myCache" default-cache="cachedb">
    <local-cache name="cachedb"/>
</cache-container>
```

To use the replicated mode in a cluster following configuration works fine:

```
<cache-container name="myCache" default-cache="cachedb">
    <transport lock-timeout="60000"/>
    <replicated-cache name="cachedb" batching="true" mode="SYNC"/>
    <replicated-cache name="messages" batching="true" mode="SYNC" />
    <replicated-cache name="answers"  batching="true" mode="SYNC" />
    <replicated-cache name="users" batching="true" mode="SYNC" />
</cache-container>
```

Refer to Infinispan and Wildfly docs for more configuration options. You can for example configure disk storage for "backup" to survive from cluster wide reboots. There are also numerous options how you can fine tune data replication or use "distribution" that will scale better in large clusters.

### 2. Start Wildfly

The perfect setup depends on your setup, but e.g. issue following command from wildfly home

./bin/standalone.sh --server-config=standalone-full-ha.xml

### 3. Build and deploy

mvn install wildfly:deploy
