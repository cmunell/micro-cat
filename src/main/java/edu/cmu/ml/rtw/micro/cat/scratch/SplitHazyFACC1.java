package edu.cmu.ml.rtw.micro.cat.scratch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

public class SplitHazyFACC1 {
	public static void main(String[] args) {
		String inputFilePath = args[0];
		String outputDirPath = args[1];

		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(inputFilePath))));
			String line = null;

			while ((line = r.readLine()) != null) {
				String[] nameAndDocument = line.split("\t");
				String name = nameAndDocument[0];
				String document = nameAndDocument[1];
				BufferedWriter w = new BufferedWriter(new FileWriter(new File(outputDirPath, name)));
				w.write(document);
				w.close();
				//System.out.println("Outputting " + name + "...");
			}
			
			r.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
