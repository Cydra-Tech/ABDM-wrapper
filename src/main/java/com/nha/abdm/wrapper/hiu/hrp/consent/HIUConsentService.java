/* (C) 2024 */
package com.nha.abdm.wrapper.hiu.hrp.consent;

import com.nha.abdm.wrapper.common.RequestManager;
import com.nha.abdm.wrapper.common.Utils;
import com.nha.abdm.wrapper.common.exceptions.IllegalDataStateException;
import com.nha.abdm.wrapper.common.models.Consent;
import com.nha.abdm.wrapper.common.responses.FacadeResponse;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.repositories.LogsRepo;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.repositories.PatientRepo;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.services.RequestLogService;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.tables.Patient;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.tables.RequestLog;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.tables.helpers.FieldIdentifiers;
import com.nha.abdm.wrapper.hip.hrp.database.mongo.tables.helpers.RequestStatus;
import com.nha.abdm.wrapper.hip.hrp.link.hipInitiated.responses.GatewayGenericResponse;
import com.nha.abdm.wrapper.hiu.hrp.consent.requests.*;
import com.nha.abdm.wrapper.hiu.hrp.consent.responses.ConsentResponse;
import com.nha.abdm.wrapper.hiu.hrp.consent.responses.ConsentStatusResponse;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.Exceptions;

@Service
public class HIUConsentService implements HIUConsentInterface {

  private static final Logger log = LogManager.getLogger(HIUConsentService.class);

  @Value("${consentInitPath}")
  private String consentInitPath;

  @Value("${consentStatusPath}")
  private String consentStatusPath;

  @Value("${consentHiuOnNotifyPath}")
  private String consentHiuOnNotifyPath;

  @Value("${fetchConsentPath}")
  private String fetchConsentPath;

  private final RequestManager requestManager;
  private final RequestLogService requestLogService;
  private final LogsRepo logsRepo;
  private final PatientRepo patientRepo;

  @Autowired
  public HIUConsentService(
      RequestManager requestManager,
      RequestLogService requestLogService,
      LogsRepo logsRepo,
      PatientRepo patientRepo) {
    this.requestManager = requestManager;
    this.requestLogService = requestLogService;
    this.logsRepo = logsRepo;
    this.patientRepo = patientRepo;
  }

  @Override
  public FacadeResponse initiateConsentRequest(InitConsentRequest initConsentRequest) {
    try {
      ResponseEntity<GatewayGenericResponse> response =
          requestManager.fetchResponseFromGateway(consentInitPath, initConsentRequest);
      if (response.getStatusCode().is2xxSuccessful()) {
        requestLogService.persistConsentInitRequest(
            initConsentRequest, RequestStatus.CONSENT_INIT_ACCEPTED, null);
      } else {
        String error =
            (Objects.nonNull(response.getBody())
                    && Objects.nonNull(response.getBody().getErrorResponse()))
                ? response.getBody().getErrorResponse().getMessage()
                : "Error from gateway while initiating consent request: "
                    + initConsentRequest.toString();
        log.error(error);
        requestLogService.persistConsentInitRequest(
            initConsentRequest, RequestStatus.CONSENT_INIT_ERROR, error);
      }
      return FacadeResponse.builder().httpStatusCode(response.getStatusCode()).build();
    } catch (Exception ex) {
      String error =
          "Exception while initiating consent request: "
              + ex.getMessage()
              + " unwrapped exception: "
              + Exceptions.unwrap(ex);
      log.error(error);
      requestLogService.persistConsentInitRequest(
          initConsentRequest, RequestStatus.CONSENT_INIT_ERROR, error);
      return FacadeResponse.builder()
          .message(error)
          .httpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
          .build();
    }
  }

  @Override
  public ConsentStatusResponse consentRequestStatus(String clientRequestId)
      throws IllegalDataStateException {

    RequestLog requestLog = logsRepo.findByClientRequestId(clientRequestId);
    if (requestLog == null) {
      throw new IllegalDataStateException(
          "Client request not found in database: " + clientRequestId);
    }
    try {
      // Check whether we have got consentRequestId as part of consent on init call from gateway.
      // If not, then send the request status as is.
      if (Objects.isNull(requestLog.getResponseDetails())
          || Objects.isNull(
              requestLog.getResponseDetails().get(FieldIdentifiers.RESPONSE_DETAILS))) {
        return ConsentStatusResponse.builder()
            .status(requestLog.getStatus())
            .httpStatusCode(HttpStatus.OK)
            .build();
      }
      if (Objects.isNull(requestLog.getResponseDetails())
          || Objects.isNull(
              requestLog.getResponseDetails().get(FieldIdentifiers.CONSENT_DETAILS_RESPONSE))) {
        return ConsentStatusResponse.builder()
            .status(requestLog.getStatus())
            .httpStatusCode(HttpStatus.OK)
            .build();
      }
      String consentRequestId =
          (String) requestLog.getResponseDetails().get(FieldIdentifiers.RESPONSE_DETAILS);
      ConsentStatusRequest consentStatusRequest =
          ConsentStatusRequest.builder()
              .requestId(UUID.randomUUID().toString())
              .timestamp(Utils.getCurrentTimeStamp())
              .consentRequestId(consentRequestId)
              .build();
      ResponseEntity<GatewayGenericResponse> response =
          requestManager.fetchResponseFromGateway(consentStatusPath, consentStatusRequest);
      if (response.getStatusCode().is2xxSuccessful()) {
        requestLogService.updateStatus(
            requestLog.getGatewayRequestId(), RequestStatus.CONSENT_STATUS_ACCEPTED);
        return ConsentStatusResponse.builder()
            .status(RequestStatus.CONSENT_STATUS_ACCEPTED)
            .httpStatusCode(HttpStatus.OK)
            .build();
      } else {
        String error =
            (Objects.nonNull(response.getBody())
                    && Objects.nonNull(response.getBody().getErrorResponse()))
                ? response.getBody().getErrorResponse().getMessage()
                : "Error from gateway while getting consent status: "
                    + consentStatusRequest.toString();
        log.error(error);
        requestLogService.updateError(
            requestLog.getGatewayRequestId(), error, RequestStatus.CONSENT_STATUS_ERROR);
        return ConsentStatusResponse.builder()
            .error(error)
            .httpStatusCode(response.getStatusCode())
            .build();
      }
    } catch (Exception ex) {
      String error =
          "Exception while fetching consent status: "
              + ex.getMessage()
              + " unwrapped exception: "
              + Exceptions.unwrap(ex);
      log.error(error);
      requestLogService.updateError(
          requestLog.getGatewayRequestId(), error, RequestStatus.CONSENT_STATUS_ERROR);
      return ConsentStatusResponse.builder()
          .error(error)
          .httpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
          .build();
    }
  }

  @Override
  public void hiuOnNotify(OnNotifyRequest onNotifyRequest) {
    try {
      ResponseEntity<GatewayGenericResponse> response =
          requestManager.fetchResponseFromGateway(consentHiuOnNotifyPath, onNotifyRequest);
      // If something goes wrong while acknowledging notification from gateway, then we can just log
      // it,
      // and we don't need to throw exception.
      if (!response.getStatusCode().is2xxSuccessful()) {
        String error =
            (Objects.nonNull(response.getBody())
                    && Objects.nonNull(response.getBody().getErrorResponse()))
                ? response.getBody().getErrorResponse().getMessage()
                : "Error from gateway while getting consent status: " + onNotifyRequest.toString();
        log.error(error);
      }
    } catch (Exception ex) {
      String error =
          "Exception while executing on notify: "
              + ex.getMessage()
              + " unwrapped exception: "
              + Exceptions.unwrap(ex);
      log.error(error);
    }
  }

  @Override
  public ConsentResponse fetchConsent(FetchPatientConsentRequest fetchPatientConsentRequest)
      throws IllegalDataStateException {
    FetchConsentRequest fetchConsentRequest = fetchPatientConsentRequest.getFetchConsentRequest();
    RequestLog requestLog = logsRepo.findByClientRequestId(fetchConsentRequest.getRequestId());
    if (requestLog == null) {
      throw new IllegalDataStateException(
          "Client request not found in database: " + fetchConsentRequest.getRequestId());
    }
    Map<String, Object> map = requestLog.getResponseDetails();
    if (Objects.isNull(map.get(FieldIdentifiers.CONSENT_DETAILS_RESPONSE))) {
      throw new IllegalDataStateException(
          "Consent Details not found in request log collection: "
              + fetchConsentRequest.getRequestId());
    }
    Patient patient =
        patientRepo.findByAbhaAddress(fetchPatientConsentRequest.getPatientAbhaAddress());
    if (patient == null) {
      throw new IllegalDataStateException(
          "Patient not found in database: " + fetchPatientConsentRequest.getPatientAbhaAddress());
    }
    List<Consent> consents = patient.getConsents();
    for (Consent consent : consents) {
      if (consent.getConsentDetail().getConsentId().equals(fetchConsentRequest.getConsentId())) {
        return ConsentResponse.builder().consent(consent).build();
      }
    }
    // If the below request was already made then upon success of on fetch request, status would be
    // CONSENT_FETCH_ACCEPTED. In that case, we should not issue another request to gateway.
    if (requestLog.getStatus() == RequestStatus.CONSENT_FETCH_ACCEPTED) {
      return ConsentResponse.builder()
          .status(RequestStatus.CONSENT_FETCH_ACCEPTED)
          .httpStatusCode(HttpStatus.OK)
          .build();
    }
    try {
      ResponseEntity<GatewayGenericResponse> response =
          requestManager.fetchResponseFromGateway(fetchConsentPath, fetchConsentRequest);
      if (response.getStatusCode().is2xxSuccessful()) {
        requestLogService.updateStatus(
            requestLog.getGatewayRequestId(), RequestStatus.CONSENT_FETCH_ACCEPTED);
        return ConsentResponse.builder()
            .status(RequestStatus.CONSENT_FETCH_ACCEPTED)
            .httpStatusCode(HttpStatus.OK)
            .build();
      } else {
        requestLogService.updateStatus(
            requestLog.getGatewayRequestId(), RequestStatus.CONSENT_FETCH_ERROR);
        return ConsentResponse.builder()
            .status(RequestStatus.CONSENT_FETCH_ERROR)
            .httpStatusCode(response.getStatusCode())
            .build();
      }
    } catch (Exception ex) {
      String error =
          "Exception while fetching consent: "
              + ex.getMessage()
              + " unwrapped exception: "
              + Exceptions.unwrap(ex);
      log.debug(error);
      requestLogService.updateStatus(
          requestLog.getGatewayRequestId(), RequestStatus.CONSENT_FETCH_ERROR);
      return ConsentResponse.builder()
          .error(error)
          .httpStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)
          .build();
    }
  }
}