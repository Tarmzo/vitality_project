package za.co.sqim.vitality.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.hibernate.validator.constraints.Email;

/**
 * @author Avuyile Malotana
 */
@Embeddable
public class EmailAddress implements Serializable
{
  private static final long serialVersionUID = 4607490383002944812L;
  
  private String originalValue;
  
  private String canonicalValue;
  
  public EmailAddress() {
    super();
  }
  
  public EmailAddress(String value) {
    super();
    setOriginalValue(value);
  }
  
  @Email
  public String getOriginalValue() {
    return originalValue;
  }
  
  public void setOriginalValue(String originalValue) {
    if(originalValue == null) {
      this.canonicalValue = null;
    }
    else {
      this.originalValue = originalValue.trim();
      this.canonicalValue = this.originalValue.toLowerCase();
    }
  }
  
  public String getCanonicalValue() {
    return canonicalValue;
  }
  
  public void setCanonicalValue(String canonicalValue) {
    this.canonicalValue = canonicalValue;
  }
}
