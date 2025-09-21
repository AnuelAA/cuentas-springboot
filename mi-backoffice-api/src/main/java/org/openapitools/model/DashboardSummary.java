package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DashboardSummary
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-09-17T12:29:00.555895+02:00[Europe/Madrid]", comments = "Generator version: 7.14.0")
public class DashboardSummary {

  private @Nullable Double totalAssets;

  private @Nullable Double totalLiabilities;

  private @Nullable Double netWorth;

  public DashboardSummary totalAssets(@Nullable Double totalAssets) {
    this.totalAssets = totalAssets;
    return this;
  }

  /**
   * Get totalAssets
   * @return totalAssets
   */
  
  @Schema(name = "totalAssets", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalAssets")
  public @Nullable Double getTotalAssets() {
    return totalAssets;
  }

  public void setTotalAssets(@Nullable Double totalAssets) {
    this.totalAssets = totalAssets;
  }

  public DashboardSummary totalLiabilities(@Nullable Double totalLiabilities) {
    this.totalLiabilities = totalLiabilities;
    return this;
  }

  /**
   * Get totalLiabilities
   * @return totalLiabilities
   */
  
  @Schema(name = "totalLiabilities", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalLiabilities")
  public @Nullable Double getTotalLiabilities() {
    return totalLiabilities;
  }

  public void setTotalLiabilities(@Nullable Double totalLiabilities) {
    this.totalLiabilities = totalLiabilities;
  }

  public DashboardSummary netWorth(@Nullable Double netWorth) {
    this.netWorth = netWorth;
    return this;
  }

  /**
   * Get netWorth
   * @return netWorth
   */
  
  @Schema(name = "netWorth", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("netWorth")
  public @Nullable Double getNetWorth() {
    return netWorth;
  }

  public void setNetWorth(@Nullable Double netWorth) {
    this.netWorth = netWorth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DashboardSummary dashboardSummary = (DashboardSummary) o;
    return Objects.equals(this.totalAssets, dashboardSummary.totalAssets) &&
        Objects.equals(this.totalLiabilities, dashboardSummary.totalLiabilities) &&
        Objects.equals(this.netWorth, dashboardSummary.netWorth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalAssets, totalLiabilities, netWorth);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DashboardSummary {\n");
    sb.append("    totalAssets: ").append(toIndentedString(totalAssets)).append("\n");
    sb.append("    totalLiabilities: ").append(toIndentedString(totalLiabilities)).append("\n");
    sb.append("    netWorth: ").append(toIndentedString(netWorth)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

