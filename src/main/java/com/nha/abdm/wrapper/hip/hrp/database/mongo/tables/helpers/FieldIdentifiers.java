/* (C) 2024 */
package com.nha.abdm.wrapper.hip.hrp.database.mongo.tables.helpers;

public class FieldIdentifiers {

  public static final String GATEWAY_REQUEST_ID = "gatewayRequestId";
  public static final String REQUEST_DETAILS = "requestDetails";
  public static final String STATUS = "status";
  public static final String ERROR = "error";
  public static final String ABHA_ADDRESS = "abhaAddress";
  public static final String CONSENTS = "consents";
  public static final String RESPONSE_DETAILS = "responseDetails";

  // Nested fields.
  public static final String LINK_RECORDS_REQUEST = "LinkRecordsRequest";
  public static final String HIP_ON_INIT_RESPONSE = "HIPOnInitResponse";
  public static final String HIP_ON_ADD_CARE_CONTEXT_RESPONSE = "HIPOnAddCareContext";
  public static final String CONSENT_REQUEST_ID = "ConsentRequestId";
  public static final String CONSENT_DETAILS_RESPONSE = "ConsentDetailsResponse";
  public static final String HIP_ON_CONFIRM_RESPONSE = "HIPOnConfirmResponse";
  public static final String DATA_NOTIFY_REQUEST = "HIPConsentNotification";
  public static final String DATA_REQUEST = "HIPHealthInformationRequest";
}
