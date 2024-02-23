package com.bsc.qa.stt.tests;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;

import com.bsc.qa.framework.base.BaseTest;
import com.bsc.qa.framework.utility.DBUtils;
import com.bsc.qa.framework.utility.ExcelUtils;
import com.relevantcodes.extentreports.LogStatus;

/**
 * Bsc834Test class is the main class to compare file Map with the db map
 * 
 * @author mkumar14
 *
 */
public class Bsc834Test extends BaseTest implements IHookable {
	/**
	 * inputFileName variable contains the path of the file
	 */
	public String inputFileName = null;

	/**
	 * xlspath as a class variable to use in all methods
	 */
	public String xlsPath;
	/**
	 * dbUtils variable is used as reference variable to connect to dbUtils
	 */
	public DBUtils dbUtils;
	/**
	 * Edi map contains all the file data
	 */
	public SortedMap<String, SortedMap<String, String>> ediMap;
	/**
	 * testCaseMap Contains the mapping data of test case name against the email
	 * id
	 */
	public Map<String, String> testCaseMap;
	/**
	 * csvDataList is used to store the values to write in to csv report
	 */
	public List<String[]> csvDataList = new ArrayList<String[]>();
	/**
	 * ssnDatalist is used to store the value to write in to CIN.csv
	 */
	public List<String[]> ssnDataList = new ArrayList<String[]>();

	/**
	 * bsc834Test constructor invokes the ediMap and intiliaze the variables
	 * 
	 * @param inputFileName
	 */
	public Bsc834Test(String inputFileName) {
		this.inputFileName = inputFileName;
		xlsPath = "src/test/resources/" + this.getClass().getSimpleName()
				+ ".xlsx";
		dbUtils = new DBUtils();
		/**
		 * get834FileData returns the 834 File data
		 */
		ediMap = new EdiBSC834Utility().get834FileData(inputFileName);
		testCaseMap = getTestcaseName();

	}

	/**
	 * This Method is to validate the MCTR reason code value fetched form
	 * database for termination transaction and eEnroll
	 * 
	 * @param method
	 */
	@Test
	public void testReasonCode(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());

		/**
		 * Looping through the ediMap each subscriber
		 */
		ediMap.keySet().forEach(
				subscriberId -> {
					String ins01 = ediMap.get(subscriberId).get("INS01");
					String ins03 = ediMap.get(subscriberId).get("INS03") == null ? "null" : ediMap.get(subscriberId).get("INS03");
					String ins04;
					// If member is dependent and INS04 is not 08
					// then consider INS04 as value of subscriber
					if (ins01.matches("N")&& !ediMap.get(subscriberId).get("INS04").matches("08")) {
						ins04 = ediMap.get(ediMap.get(subscriberId).get("MEMBER_SSN")).get("INS04");
					} else {
						ins04 = ediMap.get(subscriberId).get("INS04");
					}
					List<String> hdSegments = ediMap.get(subscriberId)
							.keySet().stream()
							.filter(key -> key.contains("HD01"))
							.collect(Collectors.toList());
					SortedMap<String, SortedMap<String, String>> queryResult = dbUtils
							.getMultiRowsFromPreparedQuery(
									"facets",
									dataMap.get("PREPARED_STATEMENT_QUERY"),
									"CSPI_ID",
									subscriberId,
									ediMap.get(subscriberId).get(
											"MEMBER_GROUPID"),
									subscriberId);
					// Looping through HD segments
					hdSegments.forEach(hdSegment -> {
						String status = "NA";

						// Condition to check whether the subscriber is
						// for termination transaction
						if (ins03.matches("024")
								&& ediMap.get(subscriberId)
								.get(hdSegment).matches("024")) {
							String reasonCodeValue = queryResult.get(
									hdSegment.substring(5, 13)).get(
											"SBEL_MCTR_RSN");
							String expectedValue;
							if (ins04.matches("08")) {

								// reason code should be HX08
								expectedValue = "HX08";
								status = reasonCodeValue
										.matches("HX08") ? "PASS"
												: "FAIL";
								softAssertions
								.assertThat(reasonCodeValue)
								.as("Actual Value : "
										+ reasonCodeValue
										+ " || Expected Value : HX08")
								.isEqualTo("HX08");
							} else {
								// reason code should be blank
								expectedValue = " ";
								status = reasonCodeValue.matches(" ") ? "PASS"
										: "FAIL";
								softAssertions
								.assertThat(reasonCodeValue)
								.as("Actual Value : "
										+ reasonCodeValue
										+ " || Expected Value : blank")
								.isEqualTo(" ");
							}
							// adding in CSV report
							String[] fieldDataArray = {
									subscriberId,
									"Reason Code_"
											+ hdSegment
											.substring(5, 13),
											"Expected Value: " + expectedValue,
											"Actual Value: " + reasonCodeValue,
											status,
											testCaseMap.get(ediMap.get(
													subscriberId).get(
															"MEMBER_EMAILID")),
											inputFileName };
							csvDataList.add(fieldDataArray);

						} else {
							System.out
							.println("INS03/HD01 segment value is not 024 i.e., not termination transaction || INS03 :: "
									+ ins03
									+ " || HD01 :: "
									+ ediMap.get(subscriberId)
									.get(hdSegment));
						}
					});

				});
	}

	/**
	 * This Method is to validate the subscriber details in the file against the
	 * database
	 * 
	 * @param method
	 */
	@Test
	public void testSubscriberDetails(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;
		/**
		 * Looping through the ediMap each subscriber
		 */
		for (String subscriberId : ediMap.keySet()) {
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);

			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"),
					subscriberId, fileLineMap.get("MEMBER_FIRSTNAME")
					.toUpperCase(), fileLineMap.get("MEMBER_LASTNAME")
					.toUpperCase(), fileLineMap
					.get("MEMBER_DTOF_BIRTH"));
			/**if file map contains the communication preference key then only
			 * it will validate the corresponding details
			 */
			if(fileLineMap.containsKey("COMMUNICATION_PREFERENCE")||fileLineMap.containsKey("EFFECTIVEDATE_COMMUNICATION_PREFERENCE")) {
				dbValueMap.putAll( dbUtils.getDataFromPreparedQuery(
						"facets", dataMap.get("FACETS_SQL_INPUT"),
						subscriberId, fileLineMap.get("MEMBER_FIRSTNAME")
						.toUpperCase(), fileLineMap.get("MEMBER_LASTNAME")
						.toUpperCase(), fileLineMap
						.get("MEMBER_DTOF_BIRTH")));
			}
			/**if file map contains the TCPA Consent key then only
			 * it will validate the corresponding details
			 */
			if(fileLineMap.containsKey("TCPA_CONSENT")||fileLineMap.containsKey("EFFECTIVEDATE_TCPA_CONSENT")) {
				dbValueMap.putAll( dbUtils.getDataFromPreparedQuery(
						"facets", dataMap.get("FACETS_SQL_EXPECTED"),
						subscriberId, fileLineMap.get("MEMBER_FIRSTNAME")
						.toUpperCase(), fileLineMap.get("MEMBER_LASTNAME")
						.toUpperCase(), fileLineMap
						.get("MEMBER_DTOF_BIRTH")));
			}
			String[] ssnDatArrray = { subscriberId, fileLineMap.get("LOBTYPE"),
					inputFileName };
			ssnDataList.add(ssnDatArrray);
			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);
			// Checking if dbValueMap is empty or not
			if (!dbValueMap.isEmpty()) {
				/**
				 * Comparing the file Map and db Map
				 */
				for (String dbLineKey : dbValueMap.keySet()) {
					String dbValue = dbValueMap.get(dbLineKey);
					String fileValue = fileLineMap.get(dbLineKey);
					// String fieldType=getFieldType(dbLineKey);
					status = fetchStatus(fileValue, dbValue);

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
		}
		// Writing ssnDataList in to csv

		CsvUtils.writeAllData(ssnDataList, method.getName());

	}

	/**
	 * This Method is to validate plan eligibility and term eligibility in the
	 * file against the database
	 * 
	 * @param method
	 */
	@Test
	public void testEligibility(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;
		/**
		 * Looping through the ediMap i.e each subscriber
		 */
		for (String subscriberId : ediMap.keySet()) {
			Map<String, String> dbActualMap = new HashMap<String, String>();
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);

			// Looping through the fileLineMap
			for (String planID : fileLineMap.keySet()) {

				/**
				 * if planID contains plan_hmo,plan_den,plan_ppo
				 */

				if (planID.matches("^PLAN_HMO|PLAN_DEN|PLAN_PPO|PLAN_VIS")) {

					String planVal = fileLineMap.get(planID);
					Map<String, String> dbValueMap = dbUtils
							.getDataFromPreparedQuery("facets",
									dataMap.get("PREPARED_STATEMENT_QUERY"),
									planVal, subscriberId);
					softAssertions.assertThat(dbValueMap.size()).isGreaterThan(
							0);
					for (String dbMap : dbValueMap.keySet()) {

						String value = null;

						if ("MEMBER_PLAN_CODE".equals(dbMap)) {
							value = dbValueMap.get(dbMap);
							dbActualMap.put(planID, value);

						} else {
							value = dbValueMap.get(dbMap);
							dbActualMap.put(planID + "_" + "EFFDT", value);

						}

					}
				}
			}

			if (!dbActualMap.isEmpty()) {
				for (String dbLineKey : dbActualMap.keySet()) {
					String dbValue = dbActualMap.get(dbLineKey);
					String fileValue = fileLineMap.get(dbLineKey);

					status = fetchStatus(fileValue, dbValue);

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
		}

	}

	/**
	 * This method is to validate the Term eligibility details in the file
	 * against the facets databaase
	 * 
	 * @param method
	 */

	@Test
	public void testTermEligibility(Method method) {

		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;
		/**
		 * Looping through the ediMap i.e each subscriber
		 */
		for (String subscriberId : ediMap.keySet()) {
			Map<String, String> dbActualMap = new HashMap<String, String>();
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);

			// Looping through the fileLineMap
			for (String planID : fileLineMap.keySet()) {

				/**
				 * if planID contains plan_hmo,plan_den,plan_ppo
				 */

				if (planID.matches("^PLAN_HMO|PLAN_DEN|PLAN_PPO|PLAN_VIS")) {

					String planVal = fileLineMap.get(planID);
					Map<String, String> dbValueMap = dbUtils
							.getDataFromPreparedQuery("facets",
									dataMap.get("PREPARED_STATEMENT_QUERY"),
									subscriberId, planVal);
					softAssertions.assertThat(dbValueMap.size()).isGreaterThan(
							0);
					for (String dbMap : dbValueMap.keySet()) {

						String value = null;

						if ("MEMBER_PLAN_CODE".equals(dbMap)) {
							value = dbValueMap.get(dbMap);
							dbActualMap.put(planID, value);

						} else {
							value = dbValueMap.get(dbMap);
							dbActualMap.put(planID + "_" + "TMTDT", value);

						}

					}
				}
			}

			if (!dbActualMap.isEmpty()) {
				for (String dbLineKey : dbActualMap.keySet()) {
					String dbValue = dbActualMap.get(dbLineKey);
					String fileValue = fileLineMap.get(dbLineKey);

					status = fetchStatus(fileValue, dbValue);

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
		}
		CsvUtils.writeAllData(csvDataList, method.getName());
	}

	/**
	 * This Method is to validate group details against the facets database
	 * 
	 * @param method
	 */
	@Test
	public void testGroup(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;

		for (String subscriberId : ediMap.keySet()) {
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);
			// if inputfileName contains PF05_TPERS
			if (inputFileName.contains("PF05_TPERS")) {
				String planNames = fileLineMap.get("PLAN_ID");
				Map<String, String> calperMap = dbUtils
						.getDataFromPreparedQuery(
								"wpr",
								dataMap.get("CROSSWALK_QUERY"),
								fileLineMap.get("MEMBER_GROUPID"),
								fileLineMap.get("MEMBER_SUBGROUPID").substring(
										0, 3), fileLineMap.get(planNames)
								.substring(0, 4));
				// Replacing the groupid, subgroupid ,classid
				fileLineMap.put("MEMBER_GROUPID",
						calperMap.get("MEMBER_GROUPID"));
				fileLineMap.put("MEMBER_SUBGROUPID",
						calperMap.get("MEMBER_SUBGROUPID"));
				fileLineMap.put("MEMBER_CLASSID",
						calperMap.get("MEMBER_CLASSID"));
			}
			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"),
					subscriberId, fileLineMap.get("MEMBER_GROUPID"));
			// SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);
			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);

			if (!dbValueMap.isEmpty()) {
				for (String dbLineKey : dbValueMap.keySet()) {

					String dbValue = dbValueMap.get(dbLineKey);
					String fileValue = fileLineMap.get(dbLineKey);
					System.out.println(fileValue);

					status = fetchStatus(fileValue, dbValue);

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
		}

	}

	/**
	 * This Method is to validate the PCP field against the facets database
	 * 
	 * @param method
	 */
	@Test
	public void testPCPScenario(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;

		for (String subscriberId : ediMap.keySet()) {

			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"),
					subscriberId);
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);
			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);

			if (!dbValueMap.isEmpty()) {
				for (String dbLineKey : dbValueMap.keySet()) {

					String dbValue = dbValueMap.get(dbLineKey);
					String fileValue = dbValueMap.get(dbLineKey);
					// String fieldType=getFieldType(dbLineKey);
					status = fetchStatus(fileValue, dbValue);
					// Object
					// testCaseName=getTestcaseName(fileLineMap.get("MEMBER_EMAILID"));

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
			// }
		}

	}

	/**
	 * This method is to validate the Meet validations against the facets
	 * database
	 * 
	 * @param method
	 */
	@Test
	public void testMeet(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());

		for (String subscriberId : ediMap.keySet()) {

			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"),
					subscriberId);
			System.out.println(dbValueMap);

			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);
		}

	}

	/**
	 * This Method is to write the subscriber ssn and subscriber id to the csv
	 * 
	 * @param method
	 */
	@Test
	public void testidNWlcmCsvWriter(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		List<String[]> cinList = new ArrayList<String[]>();

		for (String subscriberId : ediMap.keySet()) {

			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"),
					subscriberId);
			dbUtils.tearDown();
			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);

			if (!dbValueMap.isEmpty()) {
				String[] sbsIDDataArray = { dbValueMap.get("SBSB_ID"),
						dbValueMap.get("MEIA_REQ_TYPE"),
						dbValueMap.get("MEME_SSN") };
				cinList.add(sbsIDDataArray);

				System.out.println(dbValueMap);

			}
		}
		/**
		 * Writing in to the csv
		 */

		CsvUtils.writeAllData(cinList, method.getName());

	}

	/**
	 * This Method is to validate the County details against the facets database
	 * 
	 * @param method
	 */
	@Test
	public void testCounty(Method method) {
		Map<String, String> dataMap = ExcelUtils.getTestMethodData(xlsPath,
				method.getName());
		String status = null;
		/**
		 * Looping through the ediMap i.e each subscriber
		 */
		for (String subscriberId : ediMap.keySet()) {
			SortedMap<String, String> fileLineMap = ediMap.get(subscriberId);
			String zipcode = fileLineMap.get("MEMBER_ZIPCODE").substring(0, 5);

			Map<String, String> dbValueMap = dbUtils.getDataFromPreparedQuery(
					"facets", dataMap.get("PREPARED_STATEMENT_QUERY"), zipcode,
					fileLineMap.get("MEMBER_CITYNAME").toUpperCase());
			System.out.println(dbValueMap);

			softAssertions.assertThat(dbValueMap.size()).isGreaterThan(0);
			if (!dbValueMap.isEmpty()) {
				for (String dbLineKey : dbValueMap.keySet()) {

					String dbValue = dbValueMap.get(dbLineKey);
					String fileValue = dbValueMap.get(dbLineKey);

					status = fetchStatus(fileValue, dbValue);

					String[] fieldDataArray = { subscriberId, dbLineKey,
							fileValue, dbValue, status,
							testCaseMap.get(fileLineMap.get("MEMBER_EMAILID")),
							inputFileName };
					csvDataList.add(fieldDataArray);

					softAssertions
					.assertThat(dbValue)
					.as("Subscriber SSN is " + subscriberId
							+ ", Key is " + dbLineKey
							+ ",  DbValue is " + dbValue
							+ ", FileValue is " + fileValue)
					.isEqualToIgnoringCase(fileValue);
				}
			}
		}

	}

	/**
	 * @Override run is a hook before //@Test methods
	 */
	@Override
	public void run(IHookCallBack callBack, ITestResult testResult) {
		reportInit(testResult.getTestContext().getName(), testResult.getName());
		softAssertions = new SoftAssertions();
		logger.log(LogStatus.INFO, "Starting test " + testResult.getName());
		System.out.println(testResult.getName());
		callBack.runTestMethod(testResult);
		softAssertions.assertAll();
	}

	/**
	 * This Method is to fetch the status of the fields
	 * 
	 * @param fileValues
	 * @param dbValues
	 * @return
	 */
	public String fetchStatus(String fileValues, String dbValues) {
		String status = null;
		String fileValue = fileValues;
		String dbValue = dbValues.trim();

		try {
			// Checking the Status Value for CSV
			if (fileValue == null && !dbValue.isEmpty()) {
				status = "Needs Review";

			}
			// if fileValue is null or empty
			else if ((fileValue == null) && (dbValue.isEmpty())
					|| (fileValue.equals(" ")) && (dbValue.isEmpty())) {

				status = "Pass";

			}
			// if file value and db Value are equal
			else if (dbValue.equalsIgnoreCase(fileValue)) {
				status = "Pass";

			} else {
				status = "Fail";
			}

		} catch (IllegalArgumentException e) {
			status = "Fail";
			System.out.println("Exception occured!! Database value is:  "
					+ e.getMessage());
		}

		return status;

	}

	/**
	 * to Get the Test case name from the TestcaseMapping Sheet.csv
	 * 
	 * @param subscriberID
	 */
	private Map<String, String> getTestcaseName() {
		Map<String, String> testMap = new HashMap<String, String>();
		String xlsPath = "src/test/resources/"
				+ this.getClass().getSimpleName() + ".xlsx";
		String sheet = "MappingSheet";
		Object[][] columnArray1 = ExcelUtils.getTableArray(xlsPath, sheet);
		for (Object[] column : columnArray1) {

			testMap.put(column[0].toString(), column[1].toString());

		}

		return testMap;// Finally collecting Test case name from CSV file
	}

}
