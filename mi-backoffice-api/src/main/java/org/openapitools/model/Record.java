package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * Record
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-09-17T12:29:00.555895+02:00[Europe/Madrid]", comments = "Generator version: 7.14.0")
public class Record {

  private String id;

  private String assetId;

  private String liabilityId;

  private Double amount;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  public Record() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Record(String id, String assetId, String liabilityId, Double amount, LocalDate date) {
    this.id = id;
    this.assetId = assetId;
    this.liabilityId = liabilityId;
    this.amount = amount;
    this.date = date;
  }

  public Record id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  @NotNull 
  @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Record assetId(String assetId) {
    this.assetId = assetId;
    return this;
  }

  /**
   * Get assetId
   * @return assetId
   */
  @NotNull 
  @Schema(name = "assetId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("assetId")
  public String getAssetId() {
    return assetId;
  }

  public void setAssetId(String assetId) {
    this.assetId = assetId;
  }

  public Record liabilityId(String liabilityId) {
    this.liabilityId = liabilityId;
    return this;
  }

  /**
   * Get liabilityId
   * @return liabilityId
   */
  @NotNull 
  @Schema(name = "liabilityId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("liabilityId")
  public String getLiabilityId() {
    return liabilityId;
  }

  public void setLiabilityId(String liabilityId) {
    this.liabilityId = liabilityId;
  }

  public Record amount(Double amount) {
    this.amount = amount;
    return this;
  }

  /**
   * Get amount
   * @return amount
   */
  @NotNull 
  @Schema(name = "amount", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("amount")
  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public Record date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Get date
   * @return date
   */
  @NotNull @Valid 
  @Schema(name = "date", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("date")
  public LocalDate getDate() {
    return date;
  }

  public void setDate(LocalDate date) {
    this.date = date;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Record record = (Record) o;
    return Objects.equals(this.id, record.id) &&
        Objects.equals(this.assetId, record.assetId) &&
        Objects.equals(this.liabilityId, record.liabilityId) &&
        Objects.equals(this.amount, record.amount) &&
        Objects.equals(this.date, record.date);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, assetId, liabilityId, amount, date);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Record {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    assetId: ").append(toIndentedString(assetId)).append("\n");
    sb.append("    liabilityId: ").append(toIndentedString(liabilityId)).append("\n");
    sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
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

