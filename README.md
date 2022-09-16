
# Kafka Audit authorization

Simple custom authorizer to log only specified users  

## Getting Started

Compile the custom module 

```
mvn clean package -DskipTests
```

Run all services with docker-compose 

```
docker-compose up -V
```

Test with different users

```
./kafka-console-producer.sh --bootstrap-server localhost:9093 --producer.config=config/client-sasl-admin.properties --topic test
./kafka-console-producer.sh --bootstrap-server localhost:9093 --producer.config=config/client-sasl-user.properties --topic test
```


Configuration examples 

server.properties

```
super.users=User:kafka;User:admin

# Specify the class name of the authorizer 
authorizer.class.name=com.redhat.saiello.kafka.audit.AclAuditAuthorizer

# Include super users 
audit.authorizer.include.super.users=true

# Include users explicitly
audit.authorizer.include.users=User:devops;User:topic-creator

# Exclude users explicitly
audit.authorizer.exclude.users=User:kafka
```


log4j.properties

```
...

log4j.appender.auditAppender=org.apache.log4j.DailyRollingFileAppender
log4j.appender.auditAppender.DatePattern='.'yyyy-MM-dd-HH
log4j.appender.auditAppender.File=${kafka.logs.dir}/kafka-audit.log
log4j.appender.auditAppender.layout=org.apache.log4j.PatternLayout
log4j.appender.auditAppender.layout.ConversionPattern=[%d] %p %m (%c)%n

...

log4j.logger.audit=INFO, auditAppender
log4j.additivity.audit=false
```

