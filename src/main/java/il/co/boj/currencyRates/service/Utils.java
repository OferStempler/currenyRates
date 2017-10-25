package il.co.boj.currencyRates.service;

import il.co.boj.currencyRates.configuration.CurrencyRatesConfig;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
@Log4j
@Component
public class Utils {

	@Autowired
	CurrencyRatesConfig config;
	
	public int gerCurrentSequenceNumber() {
		 String currentDate =  new SimpleDateFormat("yyyyMMdd").format(new Date());
         String archivePath = config.getArchiveFolder();
         int currentSequnceNumber = 0;
         log.debug( "Getting current sequence number from archive folder:[" + archivePath + "]." );
        
         File folder = new File(archivePath);
         File[] listFiles = folder.listFiles();
             
         
         if (listFiles == null || listFiles.length == 0) {
                log.debug( "No files exists the archive folder. Starting daily sequence count from 0");
                return 0;
         } else {
        	 int temp = 0;
        	 
        	 for (File file : listFiles) {
				String fileName = file.getName();
				if (fileName.contains(currentDate)){
					String sequnceNumber = fileName.substring(20, 23);
					temp = Integer.parseInt(sequnceNumber);
					if (temp > currentSequnceNumber){
						currentSequnceNumber = temp;
					}					
				}
			}
                
         }
         if (currentSequnceNumber == 0){
        	 log.debug("No daily data files were found in archive. Starting daily sequence count from 0");
         } else {
         log.debug( "Found existing daily data files. Last sequence number:[" + currentSequnceNumber + "]." );
         }
		return currentSequnceNumber;
  }
	
	
}
