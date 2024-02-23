package com.bsc.qa.stt.test_factory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

import com.bsc.qa.stt.tests.Bsc834Test;

/**
 * Factory method to trigger simple parameter tests from Factory
 * 
 * @return
 */
public class Bsc834TestFactory {

	/**
	 * Factory method to trigger simple parameter tests from Factory
	 * 
	 * @return
	 */
	@Factory(dataProvider = "data")
	public Object[] factoryMethod(String inputFileName) {
		return new Object[] { new Bsc834Test(inputFileName) };
	}

	/**
	 * 
	 * @return
	 */
	@DataProvider(name = "data")
	public Object[] getData() {
		Object[] tableData = null;
	

		try (Stream<Path> walk = Files.walk(Paths.get(System.getenv("BSC_834_INPUT")))) {

			

			List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());

			tableData = (Object[]) result.toArray();

		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		return tableData;
	}

}
