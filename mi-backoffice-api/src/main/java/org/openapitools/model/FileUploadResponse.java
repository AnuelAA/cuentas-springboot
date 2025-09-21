package org.openapitools.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * FileUploadResponse
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-09-17T12:29:00.555895+02:00[Europe/Madrid]", comments = "Generator version: 7.14.0")
public class FileUploadResponse {

  private @Nullable String fileId;

  private @Nullable String fileName;

  /**
   * Gets or Sets status
   */
  public enum StatusEnum {
    UPLOADED("uploaded"),
    
    PROCESSING("processing"),
    
    COMPLETED("completed"),
    
    ERROR("error");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable StatusEnum status;

  @Valid
  private List<String> errors = new ArrayList<>();

  public FileUploadResponse fileId(@Nullable String fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Get fileId
   * @return fileId
   */
  
  @Schema(name = "fileId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fileId")
  public @Nullable String getFileId() {
    return fileId;
  }

  public void setFileId(@Nullable String fileId) {
    this.fileId = fileId;
  }

  public FileUploadResponse fileName(@Nullable String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * Get fileName
   * @return fileName
   */
  
  @Schema(name = "fileName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fileName")
  public @Nullable String getFileName() {
    return fileName;
  }

  public void setFileName(@Nullable String fileName) {
    this.fileName = fileName;
  }

  public FileUploadResponse status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public FileUploadResponse errors(List<String> errors) {
    this.errors = errors;
    return this;
  }

  public FileUploadResponse addErrorsItem(String errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * Get errors
   * @return errors
   */
  
  @Schema(name = "errors", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errors")
  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FileUploadResponse fileUploadResponse = (FileUploadResponse) o;
    return Objects.equals(this.fileId, fileUploadResponse.fileId) &&
        Objects.equals(this.fileName, fileUploadResponse.fileName) &&
        Objects.equals(this.status, fileUploadResponse.status) &&
        Objects.equals(this.errors, fileUploadResponse.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, fileName, status, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FileUploadResponse {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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

