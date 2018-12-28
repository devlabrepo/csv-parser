import csv.CSVParser;

public class Main {

    public static void main(String[] args) {

        CSVParser csvParser = new CSVParser();
        csvParser.csvParser("sample.csv", ',');

    }
}
