package za.co.sqim.vitality.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author Avuyile Malotana
 */
@Data
@Entity
@Table(name = "INVITATIONS")
public class UserInvitation
{
  @Id
  @GeneratedValue(strategy = IDENTITY)
  @Column(name = "ID", nullable = false, unique = true, updatable = false)
  private Long id;
  
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "SENDER_ID")
  @NotNull
  private User sender;
  
  @NotNull
  private String receiverName;
  
  @AttributeOverrides({
    @AttributeOverride(column = @Column(name = "RECEIVER_EMAIL_ORIGINAL"),
                       name = "originalValue"),
    @AttributeOverride(column = @Column(name = "RECEIVER_EMAIL_CANONICAL",
                                        unique = true),
                       name = "canonicalValue")
  })
  @NotNull
  @Valid
  private EmailAddress receiverEmailAddress = new EmailAddress();
  
  @NotNull
  private String token = UUID.randomUUID().toString();
  
  @NotNull
  private boolean accepted = false;
  
  @NotNull
  private LocalDateTime creationDateTime = LocalDateTime.now();
  
  @NotNull
  private LocalDateTime expiryDateTime = LocalDateTime.now().plusDays(7l);
  
  public UserInvitation() {
    super();
  }
  
  public UserInvitation(User sender, String name, String email) {
    this.sender = sender;
    this.receiverName = name;
    this.receiverEmailAddress.setOriginalValue(email);
  }
}
