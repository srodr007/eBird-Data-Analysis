mvn clean package
#Task 1
spark-submit --class edu.ucr.cs.cs167.srodr007.DataPreparation --master local[*] target/srodr007_projectB-1.0-SNAPSHOT.jar eBird_10k.csv

#Task3
spark-submit --class edu.ucr.cs.cs167.srodr007.TemporalAnalysis --master local[*] target/srodr007_projectB-1.0-SNAPSHOT.jar 01/17/2016 02/10/2016

#Task4
spark-submit --class edu.ucr.cs.cs167.srodr007.Task4 --master local[*] target/srodr007_projectB-1.0-SNAPSHOT.jar