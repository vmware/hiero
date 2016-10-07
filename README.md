# Hiero
Big data spreadsheet


# Installing spark and HDFS

To get a local spark and HDFS installation running, execute the following steps.

First, download a JDK for Linux x64 from here: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

Make sure to download the tarball version of the JDK.

Unpack the JDK, and set your JAVA_HOME environment variable to point to the unpacked folder (e.g, <fully qualified path to>/jdk/jdk1.8.0_101). To set your JAVA_HOME environment variable, add the following to your ~/.bashrc or ~/.zshrc.

> export JAVA_HOME="<path-to-jdk-folder>"


Next, download Spark and Hadoop. It will ask for your password.

> $: ./install.sh


Once that succeeds, configure your Hadoop installation:

> $: ./configure.sh


The above script formats your HDFS directory. It will prompt you if an existing HDFS installation exists.

Next, test whether you can SSH without a password to your local machine because Hadoop needs that to transfer files around.

> $: ssh localhost

If the above step asked for your password, run the following script to setup an SSH key just for hadoop to use. This is a one time step:

> $: ./keygen.sh

Now re-verify whether you can SSH into your local machine without a password. Do a "/etc/init.d/ssh start" if ssh complains that the remote server is down.

Lastly, start your new development Hadoop and Spark cluster:

> $: ./services.sh start

To verify whether this works, run:

> $: jps

You should see three Workers, one Datanode, one Namenode and a Master. The jps command is in $JAVA_HOME/bin/, in case your shell can't find jps.


# Running your first job

First, create a large-scale dataset with the following command:

$: echo "words\nwordywords\nwords\nword" > example_file

Let's copy that file into HDFS:

> $: hadoop-2.7.3/bin/hdfs dfs -moveFromLocal example_file /

Now it's time to run a massively parallel word count job on that petabyte scale dataset we created
above. To do that, run a spark shell:

> $: spark-2.0.0-bin-hadoop2.7/bin/spark-shell

The default installation configures HDFS to listen on localhost:54310. We need that URL
to refer to HDFS from inside the spark shell.

Let's run the word count program from the spark example

>  val textFile = sc.textFile("hdfs://localhost:54310/example_file")

>  val counts = textFile.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey(_ + _)

>  counts.saveAsTextFile("hdfs://localhost:54310/output")


We should see the result in the output folder in HDFS:

> $: hadoop-2.7.3/bin/hdfs dfs -ls /output/
> $: hadoop-2.7.3/bin/hdfs dfs -cat /output/part-00000

