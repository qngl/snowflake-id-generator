##Snowflake ID Generator
1. Empty constructor, the workId and datacenterId will be generated from the MAC address.
```java
IdGenerator generator = new SnowflakeIdGenerator();
long id = generator.nextId();
```
2. Full constructor.
```java
IdGenerator generator = new SnowflakeIdGenerator(1L, 1L);
long id = generator.nextId();
```
