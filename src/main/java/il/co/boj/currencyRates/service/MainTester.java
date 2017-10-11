//package il.co.boj.currencyRates.service;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.nio.file.FileSystem;
//import java.nio.file.FileSystems;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardWatchEventKinds;
//import java.nio.file.WatchEvent;
//import java.nio.file.WatchKey;
//import java.nio.file.WatchService;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Date;
//
//public class MainTester {
//
//	public String buildFileTrailer(int records){
//		String FT = "";
//		
//		String IDENTIFIER = "FT";
//		String NO_OF_RECORDS =  String.format("%12s",records); 
//				
//		FT = IDENTIFIER + NO_OF_RECORDS;		
//		return FT;
//	}
//	
//	public String buildHeader(){
//		String header = "";		
//		String IDENTIFIER = "FH";
//		String INSTITUTION_NUMBER = "00000002";
//		String FIELD_LABEL = "FXCURRENCY";
//		String Date = new SimpleDateFormat("yyyyMMdd").format(new Date());
//		String SEQUENCE_NO = "XXXX";
//		String LAYOUT_VERSION = "0001";
//		String FX_RATE_CATEGORY = "XXX";
//		String BASE_CURRENCY = "XXX";
//		String RATE_FORMULA = "XXX";
//		
//		header = IDENTIFIER + INSTITUTION_NUMBER + FIELD_LABEL + Date + SEQUENCE_NO + LAYOUT_VERSION + FX_RATE_CATEGORY 
//				+ BASE_CURRENCY + RATE_FORMULA;
//		
//		return header;
//	}
//	
//	public String buildBody (String currencyCodeNumbers, String dateNumber, String rateNumber){
//		
//		String body = "";
//		String charsSpace16 = String.format("%16s",""); 
////		String leftSpace ="%"+String.valueOf(16 - rateNumber.length())+ "s";
//		
//		
//		String IDENTIFIER = "RD";
//		String EFFECTIVE_DATE = dateNumber;
//		String CURRENCY = "XXX";
//		String SALES_RATE = charsSpace16;
//		String MIDDLE_RATE = String.format("%16s",rateNumber); 
//		String PURCHASE_RATE = charsSpace16;
//		String CALCULATION_BASE = "XXX";
//				
//		body = 	IDENTIFIER + EFFECTIVE_DATE + CURRENCY  + SALES_RATE+ MIDDLE_RATE
//				+ PURCHASE_RATE + CALCULATION_BASE;
//				
//		return body;
//	}
//	
//	public static void main(String[] args) {
//		
////		String x = "XXX";
////		String y = "YYY";
////		String a = String.format("%10s",x); 
////		String b =  String.format("%-10s",y); 
////		System.out.println(a+b);
//
//		
//		MainTester main = new MainTester();
//		Path inputPath = Paths.get("C:/Users/ofers/Desktop/CurrencyReates realated/listening path");
//		String outputPath = "C:/Users/ofers/Desktop/CurrencyReates realated/listening path/out" ;
//        FileSystem fileSystem = FileSystems.getDefault();
//        WatchService watchService = null;
//		try  {
//			watchService = fileSystem.newWatchService();
//        inputPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_MODIFY,
//                     StandardWatchEventKinds.ENTRY_DELETE);
//		} catch (IOException e) {
////			log.error("Could not listen to folder [" +inputPath+ "]" + e);
//		}
//        while(true){
//        	
//        	
//		try {
//			WatchKey watchKey;
//			watchKey = watchService.take();
//          List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
//          for (WatchEvent<?> we: watchEvents){
//        	  
//               if(we.kind() == StandardWatchEventKinds.ENTRY_CREATE){
////                     log.debug("Created: "+we.context());
//                     File file = new File(inputPath.toString());
//                     outputPath = outputPath +"/RS2-" + (new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date())) +".txt";
//                     File out = new File(outputPath);
//                     File[] listOfFiles = file.listFiles();
//                     for (int i = 0; i < listOfFiles.length; i++) {
//						if(listOfFiles[i].isFile()){
//							File current = listOfFiles[i];
//							String line = "";
//
//								BufferedReader reader = new BufferedReader(new FileReader(current));
//								BufferedWriter writer = new BufferedWriter(new FileWriter(out), 1000);
//								String header = main.buildHeader();
//								writer.write(header + System.getProperty("line.separator") );
//								int records = 0;
//								while ((line =reader.readLine()) != null){
//									String currencyCodeNumbers = line.substring(0, 2);
//									String dateNumber = line.substring(2, 10);
//									String rateNumber = String.valueOf((Float.parseFloat(line.substring(10, 26)))/10_000);							
//									
//									String body = main.buildBody(currencyCodeNumbers, dateNumber, rateNumber);
//									
//									writer.write(body+ System.getProperty("line.separator"));
//									records++;
//								}	
//								
//								String fileTrailer  = main.buildFileTrailer(records);
//								writer.write(fileTrailer);
//							reader.close();
//							writer.close();
//							
//						}
//					}
//                     
//                     
//               }else if (we.kind() == StandardWatchEventKinds.ENTRY_DELETE){
//                      System.out.println("Deleted: "+we.context());
//               } else if(we.kind() == StandardWatchEventKinds.ENTRY_MODIFY){
//                      System.out.println("Modified :"+we.context());
//               }
//          }
//          if(!watchKey.reset()){
//               break;
//          }
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//        }
//
//	}
//	
//}
