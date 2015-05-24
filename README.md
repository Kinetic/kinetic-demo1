Kinetic-demo1
=====


This is the beginning of a demonstration program. This program will discover drives and put up a system performance meter and a strip chart for each discovered device. 

In each device, there are some parameters and a go/stop button. 

To make the reads work, you must run the writes first. This is for each type (random/sequential), (large,small values).

How to run the Demo
=========
1. git clone https://github.com/Seagate/kinetic-demo1.git
2. cd Demo1 
3. mvn clean package
4. java -jar ./target/Demo1-0.0.1-SNAPSHOT-jar-with-dependencies.jar
