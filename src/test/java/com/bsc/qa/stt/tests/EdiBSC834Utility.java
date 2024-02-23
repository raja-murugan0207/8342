package com.bsc.qa.stt.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author mkumar14
 *
 */
public class EdiBSC834Utility {
	/**
	 * referenceMap is used to store the segment details
	 */
	public Map<String, String> referenceMap = null;
	/**
	 * singleRecordMap is used to store the subscriber details
	 */

	public SortedMap<String, String> singleRecordMap = null;

	/**
	 * This Constructor is used to store segments values from the file
	 */
	public EdiBSC834Utility() {
		referenceMap = new HashMap<String, String>();
		referenceMap.put("REF*0F", "MEMBER_SSN");
		referenceMap.put("REF*1L", "MEMBER_GROUPID");
		referenceMap.put("REF*DX", "MEMBER_SUBGROUPID");
		referenceMap.put("REF*17", "MEMBER_CLASSID");
		referenceMap.put("REF*23", "MEMBER_ID");
		referenceMap.put("REF*6O", "REF*6O");
		referenceMap.put("REF*ZZ", "REF*ZZ");
		referenceMap.put("REF*3H", "");
		referenceMap.put("REF*AB", "");
		referenceMap.put("LUI*LD", "MEMBER_LANGUAGECODE");
		referenceMap.put("LUI*LE", "MEMBER_LANGUAGECODE");
		referenceMap.put("DTP*356", "MEMBER_ORIG_EFF");
		referenceMap.put("DTP*050", "MEMBER_RECVIEDT");
		referenceMap.put("DTP*300", "MEMBER_SIGNDT");
		referenceMap.put("DTP*303", "DTP*303");
		referenceMap.put("DTP*349", "MEMBER_PLAN_TMDT");
		referenceMap.put("DTP*348", "MEMBER_PLAN_EFT");

	}

	/**
	 * This Method will return file data with uniique subscriber ssn
	 * 
	 * @param inputFileName
	 * @return FileMap
	 */

	public SortedMap<String, SortedMap<String, String>> get834FileData(
			String inputFileName) {
		SortedMap<String, SortedMap<String, String>> flatFileValuesMap = new TreeMap<String, SortedMap<String, String>>();
		// To Fetch subscriber SSN's
		List<String> subscribersList = parseBSC834FileForSubscriberid(inputFileName);
		for (String primaryKey : subscribersList) {
			singleRecordMap = new TreeMap<String, String>();
			// To capture specific subscriber section
			List<String> rowsList = parseBSC834(inputFileName, primaryKey);
			System.out.println(" Subscriber SSN used while storing flat file data: "+ primaryKey.trim().toLowerCase());
			int subscriber = 0;
			int subCity = 0;
			int rowCountLine = 0;
			String currentLine = null;
			String referenceType = null;
			int comPrefFlag = 0;
			int tcpaConsFlag = 0;

			for (String row : rowsList) {
				try {

					currentLine = row.replace("~", "");
					//Setting the flags if file contains communication preference details
					if (currentLine.matches("^N1\\*75\\*COMMUNICATION PREFERENCE.*")) {
						comPrefFlag = 1;
					} 
					if ((comPrefFlag == 1||tcpaConsFlag == 2) && currentLine.matches("^N1\\*75\\*TCPA CONSENT.*")) {
						tcpaConsFlag = 1;
						comPrefFlag = 4;
					} else if (currentLine.matches("^N1\\*75\\*TCPA CONSENT.*")) {
						tcpaConsFlag = 1;
					}
					//Setting Communication Preference and TCPA Consent value based on file value
					if (currentLine.matches("^REF\\*ZZ.*")) {
						if (comPrefFlag==1) {

							String communicationPreference = currentLine.split("\\*")[2];
							//Method to map file value based on DB value
							communicationPreferenceMapping(communicationPreference);
							comPrefFlag = 2;
						}
						else if(tcpaConsFlag == 1) {
							String tcpaConsent = currentLine.split("\\*")[2];
							//Method to map file value based on DB value
							tcpaConsentMapping (tcpaConsent);
							tcpaConsFlag = 2;	
						}
					}
					//Setting Effective Date based on file value
					if (currentLine.matches("^DTP\\*007*.*")) {

						if(tcpaConsFlag == 1||tcpaConsFlag == 2) {
							singleRecordMap.put("EFFECTIVEDATE_TCPA_CONSENT", currentLine.split("\\*")[3]);
							tcpaConsFlag = 3;	
						}
						else if (comPrefFlag == 1||comPrefFlag == 2) {
							singleRecordMap.put("EFFECTIVEDATE_COMMUNICATION_PREFERENCE", currentLine.split("\\*")[3]);
							comPrefFlag = 3;
						}
					}
					if (currentLine.startsWith("INS*")) {
						singleRecordMap.put("INS01", currentLine.split("\\*")[1]);
						singleRecordMap.put("INS03", currentLine.split("\\*")[3]);
						singleRecordMap.put("INS04", currentLine.split("\\*")[4]);
					} else if (currentLine.matches("^REF\\*0F.*|REF\\*1L.*|REF\\*DX.*|REF\\*17.*|REF\\*ZZ.*|REF\\*23.*|LUI.*")) {

						referenceType = row.substring(0, 6);
						singleRecordMap.put(referenceMap.get(referenceType),currentLine.split("\\*")[2]);

					} else if (currentLine.matches("^DTP\\*356.*|DTP\\*349*.*|DTP\\*303*.*|DTP\\*050*.*|DTP\\*348*.*")) {
						try {
							referenceType = row.substring(0, 7);
							singleRecordMap.put(referenceMap.get(referenceType),currentLine.split("\\*")[3]);
						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println(e.getMessage());
						}

					} else if (currentLine.matches("^NM1\\*IL*.*")) {

						singleRecordMap.put("MEMBER_LASTNAME",currentLine.split("\\*")[3]);
						singleRecordMap.put("MEMBER_FIRSTNAME",currentLine.split("\\*")[4]);
						singleRecordMap.put("MEMBER_MIDDLENAME",currentLine.split("\\*")[5]);
						singleRecordMap.put("SSN",currentLine.split("\\*")[9]);

					} else if (currentLine.matches("^NM1\\*P3*.*")) {

						singleRecordMap.put("MEMBER_PCPID",currentLine.split("\\*")[9]);

					} else if (currentLine.matches("^PER\\*IP.*")) {
						try {
							singleRecordMap.put("MEMBER_PHONENUMBER",currentLine.split("\\*")[4]);
							singleRecordMap.put("MEMBER_EMAILID",currentLine.split("\\*")[8]);
							singleRecordMap.put("LOBTYPE",currentLine.split("[\\_\\_]")[2]);
						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println("email id is not present");
						}
					} else if (currentLine.matches("^N3\\*.*") && subscriber == 0) {
						try {
							singleRecordMap.put("MEMBER_ADDRESS1",currentLine.split("\\*")[1]);
							singleRecordMap.put("MEMBER_ADDRESS2",currentLine.split("\\*")[2]);
						} finally {
							subscriber = subscriber + 1;
						}
					} else if (currentLine.matches("^N4\\*.*") && subCity == 0) {
						singleRecordMap.put("MEMBER_CITYNAME",currentLine.split("\\*")[1]);
						singleRecordMap.put("MEMBER_STATECODE",currentLine.split("\\*")[2]);
						singleRecordMap.put("MEMBER_ZIPCODE", currentLine.split("\\*")[3]);
						subCity = subCity + 1;
					} else if (currentLine.matches("^COB\\.*")) {
						try {

							singleRecordMap.put("COB*U",currentLine.split("\\*")[3]);

						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println("COB*U elementis not present");
						}
					} else if (currentLine.matches("^DMG\\*D8.*")) {
						singleRecordMap.put("MEMBER_DTOF_BIRTH",currentLine.split("\\*")[2]);
						singleRecordMap.put("MEMBER_SEX",currentLine.split("\\*")[3]);
						try {
							if (currentLine.split("\\*")[5].length() > 6) {
								String raceEthnicityFileVal = currentLine.split("\\*")[5].substring(5, 11).replace("~", "");
								ethinicityandRace(raceEthnicityFileVal);
							}
						} catch (ArrayIndexOutOfBoundsException e) {
							System.out.println(e.getMessage());
						}

					}
					// Added for handling multiple HD segments
					else if (currentLine.matches("^HD\\*.*")) {
						referenceType = "PLAN" + "_"
								+ currentLine.split("\\*")[3];
						singleRecordMap.put(referenceType,
								currentLine.split("\\*")[4]);
						singleRecordMap.put("HD01_"
								+ currentLine.split("\\*")[4],
								currentLine.split("\\*")[1]);
						currentLine = rowsList.get(rowCountLine + 1).toString()
								.toLowerCase();
						singleRecordMap.put(referenceType + "_" + "EFFDT",
								currentLine.split("\\*")[3].replace("~", ""));
						currentLine = rowsList.get(rowCountLine + 2).toString()
								.toLowerCase();
						singleRecordMap.put(referenceType + "_" + "TMTDT",
								currentLine.split("\\*")[3].replace("~", ""));
						singleRecordMap.put("PLAN_ID", referenceType + "_"
								+ "EFFDT");
					}
					flatFileValuesMap.put(primaryKey, singleRecordMap);
					// rowCountLine++;

				}

				catch (ArrayIndexOutOfBoundsException e) {
					System.out.println(e.getMessage());
				}
				rowCountLine++;
			}
			/**
			 * Method to set effective date if the corresponding segment is 
			 * not available only when the flag is greater than zero
			 */
			if(comPrefFlag>0) {
				dtp007CommunicationPreferenceValidation (singleRecordMap);
				flatFileValuesMap.put(primaryKey, singleRecordMap);
			}
			/**
			 * Method to set effective date if the corresponding segment is 
			 * not available only when the flag is greater than zero
			 */
			if(tcpaConsFlag>0) {
				dtp007TcpaConsentValidation (singleRecordMap);
				flatFileValuesMap.put(primaryKey, singleRecordMap);
			}
		}
		return flatFileValuesMap;
	}

	/**
	 * This Method is to capture INS*Y to INS*Y SE segment
	 * 
	 * @param inputPath
	 * @param subscriberid
	 * @return
	 */

	public List<String> parseBSC834(String inputPath, String subscriberid) {

		String line = null;

		List<String> rowsList = new ArrayList<String>();
		boolean flag = false;

		try (BufferedReader reader = Files.newBufferedReader(Paths
				.get(inputPath))) {

			for (line = reader.readLine(); line != null; line = reader
					.readLine()) {

				if (line.contains(subscriberid)) {
					flag = true;
				}

				if (line.startsWith("INS*Y*18*") || line.startsWith("INS*N")) {
					if (flag) {
						break;
					} else {
						rowsList.clear();
					}
				}
				rowsList.add(line);

			}
		} catch (IOException e) {

			System.out.println(e.getMessage());
		}

		return rowsList;
	}

	/**
	 * This Method is fetch SubscriberID from the file
	 * 
	 * @param fileName
	 * @return rowList
	 */

	public List<String> parseBSC834FileForSubscriberid(String fileName) {

		String line = null;
		List<String> rowsList = new ArrayList<String>();
		try (BufferedReader reader = Files.newBufferedReader(Paths
				.get(fileName))) {

			for (line = reader.readLine(); line != null; line = reader
					.readLine()) {
				if (line.startsWith("NM1*IL*1*")) {

					rowsList.add(line.split("\\*")[9].replace("~", ""));

				}

			}
		}

		catch (IOException e) {
			System.out.println(e.getMessage());
		}

		return rowsList;

	}

	/**
	 * To Get the mappings of Race and ethincity fields
	 * 
	 * @param raceEthnicityFileVal
	 */

	public void ethinicityandRace(String raceEthnicityFileVal) {

		HashMap<String, String> race = new HashMap<String, String>();
		// Adding elements to HashMap

		race.put("1002-5", "1000");
		race.put("2029-7", "1001");
		race.put("2054-5", "1002");
		race.put("2181-1", "1020");
		race.put("2034-7", "1003");
		race.put("2036-2", "1004");
		race.put("2086-7", "1005");
		race.put("2037-0", "1018");
		race.put("2039-6", "1006");
		race.put("2040-4", "1007");
		race.put("2041-2", "1019");
		race.put("2079-2", "1008");
		race.put("2080-0", "1011");
		race.put("2047-9", "1012");
		race.put("2106-3", "1013");
		race.put("2051-1", "1015");
		race.put("2131-1", "1014");
		race.put("2028-9", "1016");
		race.put("2191-1", "1017");

		HashMap<String, String> ethnicity = new HashMap<String, String>();

		// Adding elements to HashMap
		ethnicity.put("2182-4", "1000");
		ethnicity.put("2148-5", "1001");
		ethnicity.put("2180-8", "1002");
		ethnicity.put("2161-8", "1025");
		ethnicity.put("2157-6", "1026");
		ethnicity.put("2061-1", "1022");
		ethnicity.put("2131-1", "1021");
		ethnicity.put("2028-9", "1023");
		ethnicity.put("2191-1", "1024");

		if (race.containsKey(raceEthnicityFileVal)) {
			singleRecordMap.put("MEMBER_RACE", race.get(raceEthnicityFileVal));
		}
		if (ethnicity.containsKey(raceEthnicityFileVal)) {
			singleRecordMap.put("MEMBER_ETHNICITY",
					ethnicity.get(raceEthnicityFileVal));
		}

		else {
			System.out.println("Not a valid Race/Ethnicity value");
		}

	}
	/**
	 * to map the Communication preference value with facet db value
	 * 
	 * @param communicationValue
	 * */

	private void communicationPreferenceMapping (String communicationValue) {
		String communicationMappedValue = null;
		if ("EC".contains(communicationValue)){
			communicationMappedValue = "1000";
		} else if("PC".contains(communicationValue)) {
			communicationMappedValue = "1001";
		}
		singleRecordMap.put("COMMUNICATION_PREFERENCE", communicationMappedValue); 
	}
	/**
	 * to map the TCPA CONSENT value with facet db value
	 * 
	 * @param tcpaValue
	 * */

	private void tcpaConsentMapping (String tcpaValue) {
		String tcpaMappedValue = null;
		if ("Y".contains(tcpaValue)){
			tcpaMappedValue = "I";
		} else if("N".contains(tcpaValue)) {
			tcpaMappedValue = "O";
		}
		singleRecordMap.put("TCPA_CONSENT", tcpaMappedValue); 
	}

	/**
	 * to map the DTP_700 of Communication Preference value with facet db value
	 * 
	 * @param singleRecordMap
	 * */

	private void dtp007CommunicationPreferenceValidation (Map<String, String> singleRecordMap) {
		/**
		 * if file map is not contains Communication Preference Effective date 
		 * then we need to consider the date for DTP*348 Segment value
		 */
		if(!(singleRecordMap.containsKey("EFFECTIVEDATE_COMMUNICATION_PREFERENCE"))) {
			String value = null;
			try {
				singleRecordMap.put("EFFECTIVEDATE_COMMUNICATION_PREFERENCE", singleRecordMap.get("MEMBER_PLAN_EFT"));
				value = singleRecordMap.get("EFFECTIVEDATE_COMMUNICATION_PREFERENCE");
			}catch (Exception e) {
				System.out.println(e.getMessage());
			}
			//if DTP*348 Segment value is empty then we need to consider the date for DTP*356
			if(value == null) {
				singleRecordMap.put("EFFECTIVEDATE_COMMUNICATION_PREFERENCE", singleRecordMap.get("MEMBER_ORIG_EFF"));
			}
		}  
	}

	/**
	 * to map the DTP_700 of TCPA CONSENT value with facet db value
	 * 
	 * @param singleRecordMap
	 * */

	private void dtp007TcpaConsentValidation (Map<String, String> singleRecordMap) {
		/**if file map is not contains TCPA Effective date 
		 * then we need to consider the date for DTP*348 Segment value
		 */
		if(!(singleRecordMap.containsKey("EFFECTIVEDATE_TCPA_CONSENT"))) {
			String value = null;
			try{
				singleRecordMap.put("EFFECTIVEDATE_TCPA_CONSENT", singleRecordMap.get("MEMBER_PLAN_EFT"));
				value = singleRecordMap.get("EFFECTIVEDATE_TCPA_CONSENT");
			}catch (Exception e) {
				System.out.println(e.getMessage());
			}

			//if DTP*348 Segment value is empty then we need to consider the date for DTP*356
			if(value == null) {
				singleRecordMap.put("EFFECTIVEDATE_TCPA_CONSENT", singleRecordMap.get("MEMBER_ORIG_EFF"));
			}
		} 
	}
}
