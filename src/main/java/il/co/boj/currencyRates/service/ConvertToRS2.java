package il.co.boj.currencyRates.service;

import il.co.boj.currencyRates.model.FileData;

public interface ConvertToRS2 {

	
	public void listenToFolder();
	public boolean DecodeFileFromBase64String(FileData fileData);
	
}
