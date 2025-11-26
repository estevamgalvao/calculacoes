package com.estevam.calculacoes;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.estevam.calculacoes.asset.Asset;
import com.estevam.calculacoes.operation.Operation;
import com.estevam.calculacoes.parser.CsvParser;
import com.estevam.calculacoes.parser.exception.CsvParseException;

@SpringBootApplication
public class CalculAcoesApplication {
	
	private static final String PATH_CSV = "src/main/resources/negociacao-2024 - ordenado.csv";

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CalculAcoesApplication.class, args);

		try {
			// Parse the CSV file using CsvParser
			Map<String, Asset> assets = CsvParser.parseTradesFromCsv(PATH_CSV);
			
			// Print all assets using their toString method
			System.out.println("\n=== Assets Parsed from CSV ===");
			String[] assetNames = {"IVVB11", "GOAU4", "UNIP6", "TAEE11", "SPYI11", "POSI3"}; 
			for (Asset asset : assets.values()) {

				System.out.println(asset.toString());
				if (java.util.Arrays.asList(assetNames).contains(asset.getName())) {
						System.out.println(" ============================ ");
					for (Operation op : asset.getOperations()) {

						System.out.println("  " + op.toString());

					}
											System.out.println(" ============================ ");
				}
			}
			System.out.println("==============================\n");
		} catch (CsvParseException e) {
			System.err.println("Error parsing CSV: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
