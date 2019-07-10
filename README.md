# A step to step guide on how to use the Pulsar Spark Connector

The Pulsar Spark Connector is open source on July 9, 2019. See the source code and user guide [here](https://github.com/streamnative/pulsar-spark).

## Environment

The following example uses the Homebrew package manager to download and install software on macOS, and you can choose other package managers based on *your own requirements* and operating system.
1. Install Homebrew.
```bash
/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
```

2. Install Java 8 or a higher version.

    This example uses Homebrew to install JDK8.
```bash
brew tap adoptopenjdk/openjdk
brew cask install adoptopenjdk8
```

3.  Install Apache Spark 2.4.0 or higher.

    From the official website [download](https://www.apache.org/dyn/closer.lua/spark/spark-2.4.3/spark-2.4.3-bin-hadoop2.7.tgz) Spark 2.4.3 and decompress.
```bash
tar xvfz spark-2.4.3-bin-hadoop2.7.tgz
```

4. Download Apache Pulsar 2.4.0.

    From the official website [download](https://pulsar.apache.org/en/download/) Pulsar 2.4.0.
```bash
wget https://archive.apache.org/dist/pulsar/pulsar-2.4.0/apache-pulsar-2.4.0-bin.tar.gz
tar xvfz apache-pulsar-2.4.0-bin.tar.gz
```

5. Install Apache Maven.
```bash
brew install maven
```

6. Set up the development environment.

    This example creates a Maven project called connector-test.
    
  (1) Build a framework for a Scala project using _archetype_ provided by [Scala Maven Plugin](http://davidb.github.io/scala-maven-plugin/).
```bash
mvn archetype:generate
```
In the list that appears, select the latest version of net.alchim31.maven:scala-archetype-simple, which is currently 1.7, and specify groupId, artifactId, and version for the new project.
  This example uses:
  ```text
  groupId: com.example
  artifactId: connector-test
  version: 1.0-SNAPSHOT
```
After the above steps, a Maven Scala project framework is basically set up.

 (2) Introduce Spark, Pulsar Spark Connector dependencies in _pom.xml_ under the project root directory, and use _maven_shade_plugin_ for project packaging.
 
    a. Define the version information of the dependent package.
```xml
  <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <encoding>UTF-8</encoding>
        <scala.version>2.11.12</scala.version>
        <scala.compat.version>2.11</scala.compat.version>
        <spark.version>2.4.3</spark.version>
        <pulsar-spark-connector.version>2.4.0</pulsar-spark-connector.version>
        <spec2.version>4.2.0</spec2.version>
        <maven-shade-plugin.version>3.1.0</maven-shade-plugin.version>
  </properties>
```
    b. Introduce Spark, Pulsar Spark Connector dependencies.
```xml
    <dependency>
        <groupId>org.apache.spark</groupId>
        <artifactId>spark-core_${scala.compat.version}</artifactId>
        <version>${spark.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.spark</groupId>
        <artifactId>spark-sql_${scala.compat.version}</artifactId>
        <version>${spark.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>org.apache.spark</groupId>
        <artifactId>spark-catalyst_${scala.compat.version}</artifactId>
        <version>${spark.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>io.streamnative.connectors</groupId>
        <artifactId>pulsar-spark-connector_${scala.compat.version}</artifactId>
        <version>${pulsar-spark-connector.version}</version>
    </dependency>
```

    c. Add a Maven repository that contains _pulsar-spark-connector_.
```xml
    <repositories>
      <repository>
        <id>central</id>
        <layout>default</layout>
        <url>https://repo1.maven.org/maven2</url>
      </repository>
      <repository>
        <id>bintray-streamnative-maven</id>
        <name>bintray</name>
        <url>https://dl.bintray.com/streamnative/maven</url>
      </repository>
    </repositories>
```
      d. Package the sample class with _pulsar-spark-connector_ using _maven_shade_plugin_.
```xml
    <plugin>
          <!-- Shade all the dependencies to avoid conflicts -->
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-shade-plugin</artifactId>
          <version>${maven-shade-plugin.version}</version>
          <executions>
            <execution>
              <phase>package</phase>
              <goals>
                <goal>shade</goal>
              </goals>
              <configuration>
                <createDependencyReducedPom>true</createDependencyReducedPom>
                <promoteTransitiveDependencies>true</promoteTransitiveDependencies>
                <minimizeJar>false</minimizeJar>

                <artifactSet>
                  <includes>
                    <include>io.streamnative.connectors:*</include>
                  </includes>
                </artifactSet>
                <filters>
                  <filter>
                    <artifact>*:*</artifact>
                    <excludes>
                      <exclude>META-INF/*.SF</exclude>
                      <exclude>META-INF/*.DSA</exclude>
                      <exclude>META-INF/*.RSA</exclude>
                    </excludes>
                  </filter>
                </filters>
                <transformers>
                  <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer" />
                  <transformer implementation="org.apache.maven.plugins.shade.resource.PluginXmlResourceTransformer" />
                </transformers>
              </configuration>
            </execution>
          </executions>
        </plugin>
```

## Read from and write to Pulsar in Spark programs

The project in the example includes the following programs:
1. Read the data from Pulsar (name the app _StreamRead_).
2. Write the data to Pulsar (name the app _BatchWrite_).

### Build a stream processing job to read data from Pulsar
1. In _StreamRead_, create _SparkSession_.
```scala
val spark = SparkSession
    .builder()
    .appName("data-read")
    .config("spark.cores.max", 2)
    .getOrCreate()
```
2. In order to connect to Pulsar, you need to specify _service.url_ and _admin.url_ when building _DataFrame_ and specify the _topic_ to be read.
```scala
val ds = spark.readStream
    .format("pulsar")
    .option("service.url", "pulsar://localhost:6650")
    .option("admin.url", "http://localhost:8088")
    .option("topic", "topic-test")
    .load()
ds.printSchema()  // 打印 topic-test 的 schema 信息，验证读取成功
```

3. Output _ds_ to the console to start the job execution.
```scala
val query = ds.writeStream
    .outputMode("append")
    .format("console")
    .start()
query.awaitTermination()
```

### Write data to Pulsar
1. Similarly, in _BatchWrite_, first create _SparkSession_.
```scala
val spark = SparkSession
    .builder()
    .appName("data-sink")
    .config("spark.cores.max", 2)
    .getOrCreate()
```
2. Create a list of 1-10 and convert it to a Spark Dataset and write to Pulsar.
```scala
import spark.implicits._
spark.createDataset(1 to 10)
    .write
    .format("pulsar")
    .option("service.url", "pulsar://localhost:6650")
    .option("admin.url", "http://localhost:8088")
    .option("topic", "topic-test")
    .save()
```

### Running the program
First configure and start the single-node cluster of Spark and Pulsar, then package the sample project, and submit two jobs through _spark-submit_ respectively, and finally observe the execution result of the program.
1. Modify the log level of Spark (optional).
```bash
  cd ${spark.dir}/conf
  cp log4j.properties.template log4j.properties
```
  In the text editor, change the log level to _WARN_ .
```text
  log4j.rootCategory=WARN, console
```
2. Start the Spark cluster.
```bash
cd ${spark.dir}
sbin/start-all.sh
```
3. Modify the Pulsar WebService port to 8088 (edit `${pulsar.dir}/conf/standalone.conf`) to avoid conflicts with the Spark port.
```text
webServicePort=8088
```
4. Start the Pulsar cluster.
```bash
bin/pulsar standalone
```

5. Package the sample project.
```bash
cd ${connector_test.dir}
mvn package
```

6. Start _StreamRead_ to monitor data changes in _topic-test_.
```bash
${spark.dir}/bin/spark-submit --class com.example.StreamRead --master spark://localhost:7077 ${connector_test.dir}/target/connector-test-1.0-SNAPSHOT.jar
```

7. In another terminal window, start _BatchWrite_ to write a 1-10 digit to _topic-test_ at a time.
```bash
${spark.dir}/bin/spark-submit --class com.example.BatchWrite --master spark://localhost:7077 ${connector_test.dir}/target/connector-test-1.0-SNAPSHOT.jar
```

8. At this point, you can get a similar output in the terminal where _StreamRead_ is located.

  ```text
  root
   |-- value: integer (nullable = false)
   |-- __key: binary (nullable = true)
   |-- __topic: string (nullable = true)
   |-- __messageId: binary (nullable = true)
   |-- __publishTime: timestamp (nullable = true)
   |-- __eventTime: timestamp (nullable = true)

  Batch: 0
  +-----+-----+-------+-----------+-------------+-----------+
  |value|__key|__topic|__messageId|__publishTime|__eventTime|
  +-----+-----+-------+-----------+-------------+-----------+
  +-----+-----+-------+-----------+-------------+-----------+

  Batch: 1
  +-----+-----+--------------------+--------------------+--------------------+-----------+
  |value|__key|             __topic|         __messageId|       __publishTime|__eventTime|
  +-----+-----+--------------------+--------------------+--------------------+-----------+
  |    6| null|persistent://publ...|[08 86 01 10 02 2...|2019-07-08 14:51:...|       null|
  |    7| null|persistent://publ...|[08 86 01 10 02 2...|2019-07-08 14:51:...|       null|
  |    8| null|persistent://publ...|[08 86 01 10 02 2...|2019-07-08 14:51:...|       null|
  |    9| null|persistent://publ...|[08 86 01 10 02 2...|2019-07-08 14:51:...|       null|
  |   10| null|persistent://publ...|[08 86 01 10 02 2...|2019-07-08 14:51:...|       null|
  |    1| null|persistent://publ...|[08 86 01 10 03 2...|2019-07-08 14:51:...|       null|
  |    2| null|persistent://publ...|[08 86 01 10 03 2...|2019-07-08 14:51:...|       null|
  |    3| null|persistent://publ...|[08 86 01 10 03 2...|2019-07-08 14:51:...|       null|
  |    4| null|persistent://publ...|[08 86 01 10 03 2...|2019-07-08 14:51:...|       null|
  |    5| null|persistent://publ...|[08 86 01 10 03 2...|2019-07-08 14:51:...|       null|
  +-----+-----+--------------------+--------------------+--------------------+-----------+
  ```

So far, we've started a Pulsar and a Spark, built the framework of the sample project, and used the Pulsar Spark Connector to read data from pulsar and write data to pulsar. Get a final result in spark at last.
