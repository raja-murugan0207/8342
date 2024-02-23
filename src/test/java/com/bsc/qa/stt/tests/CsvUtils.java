package com.bsc.qa.stt.tests;

import java.io.File;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * To write the data in to the CSV file
 * 
 * @author mkumar14
 *
 */

public class CsvUtils {
	/**
	 * time stamp store the current data with time
	 */
	public static String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
	/**
	 * suite name variable get the suite name with time stamp
	 */
	public static String suiteName = System.getProperty("user.dir")
			.substring(System.getProperty("user.dir").lastIndexOf("\\") + 1);
	/**
	 * outputFile variable contains the path of the test reports
	 */
	public static String outputFile = "test-output\\BSC-reports\\" + suiteName + "_" + timestamp + ".csv";
	/**
	 * ssn file contains the path of the CSV report contains the ssn's
	 */
	public static String ssnFile = System.getenv("834_SSN_PATH") + "\\" + suiteName + "_" + "ssnlist" + "_" + timestamp
			+ ".csv";
	/**
	 * csv File contains the path of the CIN CSV report
	 */
	public static String csvFile = System.getenv("BSC_IDCARDS_CIN");

	/**
	 * To write the data in to the CSV file
	 * 
	 * @param claimid,fieldName,fieldType,EDI FileOutput,carewareDBOutput,status
	 * @param header                          fields of the CSV file
	 * @return
	 */
	public static void writeAllData(List<String[]> data, String methodName) {
		if ("testSubscriberDetails".equals(methodName)) {
			try {

				// FileWriter constructor that specifies open for appending
				CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(ssnFile)), ',');
				// if the file didn't exist then we need to write out the header line
				/*
				 * if (!alreadyExists) { writer.writeNext(header); }
				 */
				// writing data in to CSV for each column

				writer.writeAll(data);

				// closing writer connection
				writer.close();
			} catch (IOException e) {

				System.out.println(e.getMessage());
			}
		} else if ("testTermEligibility".equals(methodName)) {

			boolean alreadyExists = new File(outputFile).exists();
			String[] columnArray = { "SubscriberSSN", "Key", "Expected Filevalue", "Actual FacetDBvalue", "Status",
					"TestCase Name", "Filename" };

			try {
				if (!alreadyExists) {
					CSVWriter writer1 = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFile)), ',');
					writer1.writeNext(columnArray);
					writer1.close();
				}
				// FileWriter constructor that specifies open for appending
				CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(outputFile),StandardOpenOption.APPEND), ',');

				// writing data in to CSV for each column

				writer.writeAll(data);

				// closing writer connection
				writer.close();
			} catch (IOException e) {

				System.out.println(e.getMessage());
			}
			
		} else if ("testidNWlcmCsvWriter".equals(methodName)) {
			System.out.println("the path" + csvFile);

			boolean alreadyExists = new File(csvFile).exists();
			try {

				CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(csvFile)), '-',
						CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

				if (alreadyExists) {
					writer.flush();
				}

				writer.writeAll(data);

				// closing writer connection
				writer.close();
			} catch (IOException e) {

				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * To read the data in to the CSV file
	 * 
	 * @param filepath
	 * 
	 * @return List of data in the csv
	 */
	public static List<String[]> readAllDataAtOnce(String file) {
		List<String[]> allData = null;
		try {
			// create csvReader object and skip first Line
			CSVReader csvReader = new CSVReaderBuilder(Files.newBufferedReader(Paths.get(file))).withSkipLines(1)
					.build();
			allData = csvReader.readAll();

		}

		catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return allData;

	}
}
