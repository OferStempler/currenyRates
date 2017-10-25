package il.co.boj.currencyRates.service.serviceImpl;

import il.co.boj.currencyRates.configuration.CurrencyRatesConfig;
import il.co.boj.currencyRates.model.FileData;
import il.co.boj.currencyRates.model.PrepaidResponse;
import il.co.boj.currencyRates.service.ConvertToRS2;
import il.co.boj.currencyRates.service.Utils;
import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Component
@Log4j
public class ConvertToRS2Impl implements ConvertToRS2 {

	
	
	@Autowired
	CurrencyRatesConfig config;
	
	@Autowired
	Utils utils;
	
	@Autowired
	private Map<String, String> currencyMap;
	boolean succsess = true;
	final String CURRENCY = "CURRENCY";
	
	// HEADER PARAMETERS 
	final String HEADER_IDENTIFIER = "FH";
	final String INSTITUTION_NUMBER = "00000007";
	final String FIELD_LABEL = "FXCURRENCY";
	final String LAYOUT_VERSION = "0001";
	final String FX_RATE_CATEGORY = "001";
	final String BASE_CURRENCY = "376";
	final String RATE_FORMULA = "000";
	
	String SEQUENCE_NO = "";
	
	// FOOTER PARAMETERS
	final String TRAILER_IDENTIFIER = "FT";
	
	// BODY PARAMETERS
	final String charsSpace16 = String.format("%16s","");
	final String BODY_IDENTIFIER = "RD";
	final String SALES_RATE = charsSpace16;
	final String PURCHASE_RATE = charsSpace16;
	final String CALCULATION_BASE = "000";
	
	
	String yesterday = new SimpleDateFormat("yyyyMMdd").format(new Date());
	int sequenceIdCounter = 0;
	
	@PostConstruct
	private void init(){
		log.debug("Checking existense of all critical properties:");
		Map<String, String> propMap = new HashMap<>(); 
		propMap.put("InputFolder", config.getInputFolder());
		propMap.put("OutputFolder", config.getOutPutFolder());
		propMap.put("InputArchiveFolder", config.getArchiveFolder());
		propMap.put("DestinationFolder", config.getDestination());
		propMap.put("FailedFolder", config.getFailedFolder());
		propMap.put("ErrorFolder", config.getErrorFolder());

	
		propMap.forEach((k,v) -> {
			if(v.equals(null) || v.equals("")){
				log.error("!!!FAILED TO LOAD!!!");
			log.error("Can not find critical property [" +k+ "].  Make sure all properties are exists: "
					+ "CurrencyRates.InputFolder"
					+ "CurrencyRates.OutPutFolder"
					+ "CurrencyRates.ArchiveFolder"
					+ "CurrencyRates.Destination"
					+ "CurrencyRates.FailedFolder"
					+ "CurrencyRates.ErrorFolder");
				System.exit(0);
			} else {
				log.debug(k + ": " + v);
			}
		});
		
		 sequenceIdCounter = utils.gerCurrentSequenceNumber();
		
	}
	
	@Override
	public void listenToFolder() {
		log.debug("$$$$$$ Starting CurrencyRates App $$$$$$$$$$");
		Path inputPath = Paths.get(config.getInputFolder());
        FileSystem fileSystem = FileSystems.getDefault();
        WatchService watchService = null;
        log.debug("Listening to folder inputPath [" +inputPath+ "]");
		try {
			//listen to the input folder for events like CREATE, (can also do MODIFY and DELETE)
        watchService = fileSystem.newWatchService();
        inputPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE
//        				  ,StandardWatchEventKinds.ENTRY_MODIFY,
//                          StandardWatchEventKinds.ENTRY_DELETE
        		);
			while (true) {
				WatchKey watchKey;
				log.debug("Waiting for events.");
				watchKey = watchService.take();
				List<WatchEvent<?>> watchEvents = watchKey.pollEvents();
				for (WatchEvent<?> we : watchEvents) {

					// if a file was created
					if (we.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						log.debug("==============================================================================");
						log.debug("A new file was created: " + we.context());
						String fileName = we.context().toString();
						File current = new File(inputPath.toString() + "//" + fileName);
						
						this.proccessFile(current);
					
					} 
//					else if (we.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
//						// log.debug("Deleted: "+we.context());
//					} else if (we.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
//						// log.debug("Modified :"+we.context());
//					}
					if (!watchKey.reset()) {
						break;
					}
				}
			}
		} catch (Exception e) {
			log.error("Could not find RS2 File" + e);
		}
	}
	
    private void proccessFile(File current){          
                     
		try {
			if (current.isFile()) {
				String outputPath = config.getOutPutFolder();
				outputPath = outputPath + "/bw_boj_fx_" + (new SimpleDateFormat("yyyyMMdd").format(new Date()))
						+ ".dat";
				
				File outputFile = new File(outputPath);
				

				String line = "";
				BufferedReader reader = new BufferedReader(new FileReader(current));
				BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile), 1024);
				// build the header
				String header = this.buildHeader();
				writer.write(header + System.getProperty("line.separator"));
				log.debug(header);
				// read to message body
				int records = 0;
				while ((line = reader.readLine()) != null) {
					String currencyCodeNumbers = line.substring(0, 2);
					String dateNumber = line.substring(2, 10);
					String rateNumber = String.valueOf((Float.parseFloat(line.substring(10, 26))) / 10_000);
					// build the RS2 body
					String body = this.buildBody(currencyCodeNumbers, dateNumber, rateNumber, outputFile);
					if (body == null) {
//					succsess = false;
						continue;
					} else {
					writer.write(body + System.getProperty("line.separator"));
					records++;
					log.debug(body);
				}
				}
				// build the Trailer
				String fileTrailer = this.buildFileTrailer(records);
				writer.write(fileTrailer);
				log.debug(fileTrailer);
				reader.close();
				writer.close();

				// check if file was created properly
//				if (succsess) {
					// create a base64 string from the file
					String toBase64 = encodeFiletoBase64String(outputFile);

					// Post for url destination
					FileData fileData = new FileData();
					fileData.setContent(toBase64);
					fileData.setType(CURRENCY);
					fileData.setName(outputFile.getName());
					succsess = sendBase64(fileData);

//				}
				// move the file to archive/failed folder, and delete from input
					this.moveAndDelete(current, succsess);
				
			}
		} catch (Exception e){
			log.error("Could not build RS2 File" + e);
		}
	}
	
	private boolean sendBase64(FileData fileData) {
		boolean sent = true;
		String uri = config.getDestination();
		RestTemplate template = new RestTemplate();
		template.getMessageConverters().add(0, new StringHttpMessageConverter( Charset.forName("UTF-8")));
		PrepaidResponse response = template.postForObject(uri, fileData, PrepaidResponse.class );
		String ok = response.getErrorCode();
		if(ok.equals("0")){
			log.debug("Client successfuly received base64 content");
		} else {
			sent = false;
			log.error("Client did not receive base64 content. errorCode: [" +response.getErrorCode()+ "] errorMessage [" +response.getErrorMessage()+"]");
		}
		return sent;
	}		

	

	private String encodeFiletoBase64String(File outputFile) {
		String base64 = "";
		try{
		FileInputStream input = new FileInputStream(outputFile);
		byte data[] = new byte[(int) outputFile.length()];
		input.read(data);
		
		byte[] encoded = Base64.encodeBase64(data);
		 base64 = new String(Base64.encodeBase64(data), "UTF-8");
		 
//		System.out.println(base64);
		}catch(Exception e){
			log.error("Could not encode file to base64" + e);
		}
		return base64;
	}

	private String buildFileTrailer(int records){
		String FT = "";
		
		String NO_OF_RECORDS =  String.format("%12s",records); 
				
		FT = TRAILER_IDENTIFIER + NO_OF_RECORDS;		
		return FT;
	}

	private String buildHeader(){
		String header= "";
		String Date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		String SEQUENCE_NO = doSequenceId();
		
		header = HEADER_IDENTIFIER + INSTITUTION_NUMBER + FIELD_LABEL + Date + SEQUENCE_NO + LAYOUT_VERSION + FX_RATE_CATEGORY 
				+ BASE_CURRENCY + RATE_FORMULA;
		
		return header;
	}

	private String doSequenceId(){
		//make sure that every day the sequenceId will start from 001.	The Date and the sequenceId combination must be unique.	
		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		if(today.equals(yesterday)){
			sequenceIdCounter++;//make seq counter start from the number of todays files
		} else {
			sequenceIdCounter = 1;
			yesterday = today;
		}
		
		SEQUENCE_NO = String.format("%04d",sequenceIdCounter);
		return SEQUENCE_NO;
	}
	
		private String buildBody (String currencyCodeNumbers, String dateNumber, String rateNumber, File outputFile){
			
		String body = "";
		
		String EFFECTIVE_DATE = dateNumber;
		String CURRENCY = currencyMap.get(currencyCodeNumbers); 
		if (CURRENCY == null){
			log.error("Could not find currency value for key [" +currencyCodeNumbers+"]" );
			writeToErrorFile(currencyCodeNumbers, outputFile);
			return null;
		}
		String MIDDLE_RATE = String.format("%16s",rateNumber); 
		
				
		body = 	BODY_IDENTIFIER + EFFECTIVE_DATE + CURRENCY  + SALES_RATE+ MIDDLE_RATE
				+ PURCHASE_RATE + CALCULATION_BASE;
				
		return body;
	}

		private void writeToErrorFile(String currency, File outputFile){
			
		
			try {
				String fileNameNoExtenstion = FilenameUtils.removeExtension(outputFile.getName());
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

				String errorFilePath = config.getErrorFolder() + fileNameNoExtenstion + SEQUENCE_NO + "_error.txt";
				BufferedWriter writer = new BufferedWriter(new FileWriter(errorFilePath), 1024);
				String error = date + " Could not find currency value for key [" +currency+"] for file [" +outputFile.getName()+ "] "
						+ "SEQUENCE_NO: + [" +SEQUENCE_NO+"]";
				writer.write(error + System.getProperty("line.separator"));
				writer.close();
				log.debug("Unfound currency was written to error file");
			} catch (IOException e) {
				log.error("Could not write to error file: " + e);
			}
		}
		
		private void moveAndDelete(File filetoRemove, boolean sent) {
			String archivePath = config.getArchiveFolder(); 
			String failedPath = config.getFailedFolder();
			String path = "";
			String Date = new SimpleDateFormat("yyyyMMdd").format(new Date());
			//if the file was successfully sent, send to archive. Else, sent to failed dir.
			if(sent){
			 path = archivePath+"bw_boj_fx_" +Date + "_"+ SEQUENCE_NO + ".txt";
			}else {
			 path = failedPath+"bw_boj_fx_" +Date + "_"+ SEQUENCE_NO + ".txt";
			}
			
			File archive = new File(path);
			try {
				InputStream inputStream = new FileInputStream(filetoRemove);
				OutputStream outPut = new FileOutputStream(archive);
				
				byte[] buffer = new byte[1024];
				int length;
				while((length = inputStream.read(buffer)) > 0){
					outPut.write(buffer, 0 , length);
				}
				inputStream.close();
				outPut.close();
				
				filetoRemove.delete();
				if(sent){
				log.debug("File [" +filetoRemove.getName()+ "] was successfully copied to archive directory [" +archive.getName()+ "] and was removed from input folder");
				}
				else {
				log.debug("File [" +filetoRemove.getName()+ "] was successfully copied to Failed directory [" +archive.getName()+ "] and was removed from input folder");
				}
				
			} catch (Exception e) {
				log.error("Could not copy and delete file [" +filetoRemove.getName()+"]");
				e.printStackTrace();
			}
			
		}

		
		@Override
	// this is a method for testing the encoding
	public boolean DecodeFileFromBase64String(FileData fileData) {
			
		boolean ok =true;	
		String filePath = config.getTestDecodingFolder() + fileData.getName();
		try {
			byte[] binary = DatatypeConverter.parseBase64Binary(fileData.getContent());
			FileOutputStream output = new FileOutputStream(new File(filePath));
			output.write(binary);
			output.close();
			log.error("Client testing for decoding base64 was successfull");
		} catch (Exception e) {
			log.error("Could not encode file to base64" + e);
			ok= false;
		}
		return ok;
	}
		
		
}
