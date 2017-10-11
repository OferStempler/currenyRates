package il.co.boj.currencyRates.service.serviceImpl;

//import static org.hamcrest.CoreMatchers.nullValue;

import il.co.boj.currencyRates.configuration.CurrencyRatesConfig;
import il.co.boj.currencyRates.model.FileData;
import il.co.boj.currencyRates.model.PrepaidResponse;
import il.co.boj.currencyRates.service.ConvertToRS2;
import lombok.extern.log4j.Log4j;
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

	final String CURRENCY = "CURRENCY";
	
	// HEADER PARAMETERS 
	final String HEADER_IDENTIFIER = "FH";
	final String INSTITUTION_NUMBER = "00000002";
	final String FIELD_LABEL = "FXCURRENCY";
	final String LAYOUT_VERSION = "0001";
	final String FX_RATE_CATEGORY = "XXX";
	final String BASE_CURRENCY = "XXX";
	final String RATE_FORMULA = "XXX";
	
	// FOOTER PARAMETERS
	final String TRAILER_IDENTIFIER = "FT";
	
	// BODY PARAMETERS
	final String charsSpace16 = String.format("%16s","");
	final String BODY_IDENTIFIER = "RD";
	final String SALES_RATE = charsSpace16;
	final String PURCHASE_RATE = charsSpace16;
	
	@PostConstruct
	private void init(){
		log.debug("Checking existense of all critical properties:");
		Map<String, String> propMap = new HashMap<>(); 
		propMap.put("InputFolder", config.getInputFolder());
		propMap.put("OutputFolder", config.getOutPutFolder());
		propMap.put("InputArchiveFolder", config.getArchiveFolder());
		propMap.put("DestinationFolder", config.getDestination());
		propMap.put("FailedFolder", config.getFailedFolder());

	
		propMap.forEach((k,v) -> {
			if(v.equals(null) || v.equals("")){
				log.error("!!!FAILED TO LOAD!!!");
			log.error("Can not find critical property [" +k+ "].  Make sure all properties are exists: "
					+ "CurrencyRates.InputFolder"
					+ "CurrencyRates.OutPutFolder"
					+ "CurrencyRates.ArchiveFolder"
					+ "CurrencyRates.Destination"
					+ "CurrencyRates.FailedFolder");
				System.exit(0);
			} else {
				log.debug(k + ": " + v);
			}
		});
		
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
                     if (current.isFile()){
             		 String outputPath = config.getOutPutFolder();
                     outputPath = outputPath +"/RS2-" + (new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date())) +".txt";
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
								String body = this.buildBody(currencyCodeNumbers, dateNumber, rateNumber);

								writer.write(body + System.getProperty("line.separator"));
								records++;
								log.debug(body);
							}
								//build the Trailer
								String fileTrailer  = this.buildFileTrailer(records);
								writer.write(fileTrailer);
								log.debug(fileTrailer);
							reader.close();
							writer.close();
							
							//create a base64 string from the file
							String toBase64 = encodeFiletoBase64String(outputFile) ;
							
							//Post for url destination
							FileData fileData = new FileData();
							fileData.setContent(toBase64);
							fileData.setType(CURRENCY);
							fileData.setName(outputFile.getName());
							boolean sent = sendBase64(fileData);
							
							
							//move the file to archive and delete from input
							this.moveAndDelete(current, sent);
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
		String SEQUENCE_NO = "XXXX";
		String Date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		
		header = HEADER_IDENTIFIER + INSTITUTION_NUMBER + FIELD_LABEL + Date + SEQUENCE_NO + LAYOUT_VERSION + FX_RATE_CATEGORY 
				+ BASE_CURRENCY + RATE_FORMULA;
		
		return header;
	}

		private String buildBody (String currencyCodeNumbers, String dateNumber, String rateNumber){
			
		String body = "";
		
		String EFFECTIVE_DATE = dateNumber;
		String CURRENCY = "XXX";
		String MIDDLE_RATE = String.format("%16s",rateNumber); 
		String CALCULATION_BASE = "XXX";
				
		body = 	BODY_IDENTIFIER + EFFECTIVE_DATE + CURRENCY  + SALES_RATE+ MIDDLE_RATE
				+ PURCHASE_RATE + CALCULATION_BASE;
				
		return body;
	}

		
		private void moveAndDelete(File filetoRemove, boolean sent) {
			String archivePath = config.getArchiveFolder(); 
			String failedPath = config.getFailedFolder();
			String path = "";
			String Date = new SimpleDateFormat("yyyy-MM-dd-HH.mm.ss").format(new Date());
			//if the file was successfully sent, send to archive. Else, sent to failed dir.
			if(sent){
			 path = archivePath+"input-" +Date + ".txt";
			}else {
			 path = failedPath+"input-" +Date + ".txt";
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
	// this is method for testing the encoding
	public boolean DecodeFileFromBase64String(FileData fileData) {
			
		boolean ok =true;	
		String filePath = config.getTestDecodingFolder() + fileData.getName();
		try {
			byte[] binary = DatatypeConverter.parseBase64Binary(fileData.getContent());
			FileOutputStream output = new FileOutputStream(new File(filePath));
			output.write(binary);
			output.close();
			log.debug("Client testing for decoding base64 was successfull");
		} catch (Exception e) {
			log.error("Could not encode file to base64" + e);
			ok= false;
		}
		return ok;
	}
		
		
}
