package com.nha.abdm.wrapper.hrp.responseController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nha.abdm.wrapper.hrp.serviceImpl.LogsTableService;
import com.nha.abdm.wrapper.hrp.discoveryLinking.responses.ConfirmResponse;
import com.nha.abdm.wrapper.hrp.discoveryLinking.responses.DiscoverResponse;
import com.nha.abdm.wrapper.hrp.discoveryLinking.responses.InitResponse;
import com.nha.abdm.wrapper.hrp.hipInitiatedLinking.responses.OnAddCareContextResponse;
import com.nha.abdm.wrapper.hrp.hipInitiatedLinking.responses.OnConfirmResponse;
import com.nha.abdm.wrapper.hrp.hipInitiatedLinking.responses.OnInitResponse;
import com.nha.abdm.wrapper.hrp.serviceImpl.CareContextTableService;
import com.nha.abdm.wrapper.hrp.controller.GatewayController;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Component
@RestController
public class GatewayCallbackController {
	@Autowired
	CareContextTableService careContextTableService;
	@Autowired
	GatewayController gatewayController;

	@Autowired
	LogsTableService logsTableService;
	private static final Logger log = LogManager.getLogger(GatewayCallbackController.class);


	@PostMapping({"/v0.5/users/auth/on-init"})
	public void onInitResponse(@RequestBody OnInitResponse data) throws IOException, URISyntaxException {
		log.info("getError in OnInitRequest callback: " + data.getError());
		if (data != null && data.getError() == null) {
			log.info(data.printData());
			log.info(data.getAuth().getTransactionId());
			this.careContextTableService.setTransactionId(data);
			log.info("Stored TransactionId");
			this.gatewayController.startConfirmCall(data);
		} else {
			log.info("Error in onInitCall: " + data.getError());
		}

	}

	@PostMapping({"/v0.5/users/auth/on-confirm"})
	public void onConfirmCall(@RequestBody OnConfirmResponse data) throws IOException, URISyntaxException {
		if (data != null && data.getError() == null) {
			log.info(data.printData());
			log.info("onConfirm : " + data.getAuth().getAccessToken());
			this.careContextTableService.setAccessToken(data);
			log.info("Stored AccessToken");
			data.printData();
			log.info("starting to add CareContext");
			this.gatewayController.startAddCareContextCall(data);
		}else log.info("failed in on-confirm");
	}

	@PostMapping({"/v0.5/links/link/on-add-contexts"})
	public void onAddCareContext(@RequestBody OnAddCareContextResponse data) {
		if (data != null && data.getError() == null) {
			log.info("Linked CareContext STATUS :" + data.getAcknowledgement().getStatus());
			this.careContextTableService.setStatus(data.getResp().getRequestId(), data.getAcknowledgement().getStatus());
		} else {
			log.info("Failed to add Context");
		}

	}

	@PostMapping("/v0.5/care-contexts/discover")
	public void discoverCall(@RequestBody DiscoverResponse data) throws URISyntaxException, JsonProcessingException {
		if(data!=null){
			log.info(data.printData());
			gatewayController.startOnDiscoverCall(data);
		}

	}
	@PostMapping("/v0.5/links/link/init")
	public void initCall(@RequestBody InitResponse data) throws URISyntaxException, JsonProcessingException {
		if(data!=null){
			log.info(data.printData());
			gatewayController.startOnInitCall(data);
		}
	}
	@PostMapping("/v0.5/links/link/confirm")
	public void confirmCall(@RequestBody ConfirmResponse data) throws URISyntaxException, JsonProcessingException {
		if(data!=null){
//			log.info(data.printData());
			gatewayController.startOnConfirmCall(data);
		}
	}
}
