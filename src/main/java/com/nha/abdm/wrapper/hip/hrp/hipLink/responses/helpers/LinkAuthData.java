/* (C) 2024 */
package com.nha.abdm.wrapper.hip.hrp.hipLink.responses.helpers;

import lombok.Data;

@Data
public class LinkAuthData {
  private String accessToken;
  private String transactionId;
  private String mode;
}
